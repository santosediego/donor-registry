package com.santosediego.donorregistry.auth.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.text.Normalizer;
import java.util.Locale;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import com.santosediego.donorregistry.support.TestPasswords;

class PasswordHasherTest {

	/** Algorithm and work factor, as written at the start of every hash. */
	private static final String ARGON2_PREFIX = "$argon2id$v=19$m=19456,t=2,p=1$";

	private final PasswordHasher hasher = new PasswordHasher();

	@Test
	@DisplayName("Dado uma senha, quando gera o hash, então ele é diferente da senha, é Argon2id com os parâmetros combinados e cabe na coluna")
	void hashIsArgon2idAndDoesNotContainThePassword() {
		// Arrange
		String password = TestPasswords.next();

		// Act
		String hash = hasher.hash(password);

		// Assert
		assertThat(hash).startsWith(ARGON2_PREFIX).doesNotContain(password).hasSizeLessThanOrEqualTo(255);
	}

	@Test
	@DisplayName("Dado a mesma senha, quando gera o hash duas vezes, então os hashes são diferentes (salt) e os dois conferem")
	void samePasswordGivesDifferentHashesThatBothMatch() {
		// Arrange
		String password = TestPasswords.next();

		// Act
		String first = hasher.hash(password);
		String second = hasher.hash(password);

		// Assert
		assertThat(first).isNotEqualTo(second);
		assertThat(hasher.matches(password, first)).isTrue();
		assertThat(hasher.matches(password, second)).isTrue();
	}

	@Test
	@DisplayName("Dado um hash, quando verifica a senha certa, uma errada e a certa com outra caixa, então só a certa confere")
	void onlyTheExactPasswordMatches() {
		// Arrange
		String password = TestPasswords.next();
		String hash = hasher.hash(password);

		// Act
		boolean right = hasher.matches(password, hash);
		boolean wrong = hasher.matches(TestPasswords.next(), hash);
		boolean otherCase = hasher.matches(password.toUpperCase(Locale.ROOT), hash);

		// Assert
		assertThat(right).isTrue();
		assertThat(wrong).isFalse();
		assertThat(otherCase).isFalse();
	}

	@ParameterizedTest(name = "[{0}]")
	@ValueSource(strings = { "ação, coração e pão de queijo", "senha com emoji 😀🔐 no meio", "!@#$%&*()_+-=[]{};:'\",.<>/?\\|`~" })
	@DisplayName("Dado uma senha com acento, emoji ou caracteres especiais, quando verifica, então confere")
	void matchesAccentsEmojisAndSymbols(String password) {
		// Arrange
		String hash = hasher.hash(password);

		// Act
		boolean matches = hasher.matches(password, hash);

		// Assert
		assertThat(matches).isTrue();
	}

	@Test
	@DisplayName("Dado a mesma senha em NFC e em NFD, quando gera o hash com uma forma e verifica com a outra, então confere nos dois sentidos")
	void nfcAndNfdFormsOfThePasswordMatch() {
		// Arrange
		String composed = Normalizer.normalize("coração valente de teste", Normalizer.Form.NFC);
		String decomposed = Normalizer.normalize(composed, Normalizer.Form.NFD);
		assertThat(decomposed).isNotEqualTo(composed);

		// Act
		boolean decomposedAgainstComposed = hasher.matches(decomposed, hasher.hash(composed));
		boolean composedAgainstDecomposed = hasher.matches(composed, hasher.hash(decomposed));

		// Assert
		assertThat(decomposedAgainstComposed).isTrue();
		assertThat(composedAgainstDecomposed).isTrue();
	}

	@Test
	@DisplayName("Dado duas senhas longas que só diferem no último caractere (bem depois de 72 bytes), quando verifica uma contra o hash da outra, então não confere")
	void passwordsThatDifferOnlyAtTheEndAreNotEqual() {
		// Arrange: 128 characters, the longest allowed
		String start = "ab".repeat(63) + "a";
		String first = start + "1";
		String second = start + "2";
		String hash = hasher.hash(first);

		// Act
		boolean sameMatches = hasher.matches(first, hash);
		boolean otherMatches = hasher.matches(second, hash);

		// Assert
		assertThat(sameMatches).isTrue();
		assertThat(otherMatches).isFalse();
	}

	@Test
	@DisplayName("Dado uma senha acima de 128 caracteres, quando gera o hash ou verifica, então é recusada e o erro não repete a senha")
	void refusesPasswordLongerThanTheMaximum() {
		// Arrange
		String tooLong = "xy".repeat(64) + "z";
		String hashOfTheAllowedPart = hasher.hash(tooLong.substring(0, 128));

		// Act
		Throwable thrown = catchThrowable(() -> hasher.hash(tooLong));
		boolean matches = hasher.matches(tooLong, hashOfTheAllowedPart);

		// Assert
		assertThat(thrown).isInstanceOf(IllegalArgumentException.class);
		assertThat(thrown.getMessage()).doesNotContain(tooLong).doesNotContain("xyxy");
		assertThat(matches).isFalse();
	}

	@ParameterizedTest(name = "[{0}]")
	@NullAndEmptySource
	@DisplayName("Dado uma senha nula ou vazia, quando gera o hash ou verifica, então é recusada sem NullPointerException")
	void refusesMissingPassword(String password) {
		// Arrange
		String hash = hasher.hash(TestPasswords.next());

		// Act
		Throwable thrown = catchThrowable(() -> hasher.hash(password));
		boolean matches = hasher.matches(password, hash);

		// Assert
		assertThat(thrown).isInstanceOf(IllegalArgumentException.class);
		assertThat(matches).isFalse();
	}

	static Stream<Arguments> malformedHashes() {
		return Stream.of(
			corruption("cortado ao meio", hash -> hash.substring(0, hash.length() / 2)),
			corruption("sem o trecho final", hash -> hash.substring(0, hash.lastIndexOf('$'))),
			corruption("com o trecho final curto demais", hash -> hash.substring(0, hash.lastIndexOf('$') + 4)),
			corruption("com versão desconhecida do Argon2", hash -> hash.replace("$v=19$", "$v=18$")),
			corruption("vazio", hash -> "")
		);
	}

	private static Arguments corruption(String name, UnaryOperator<String> change) {
		return Arguments.of(Named.of(name, change));
	}

	@ParameterizedTest(name = "hash {0}")
	@MethodSource("malformedHashes")
	@ExtendWith(OutputCaptureExtension.class)
	@DisplayName("Dado um hash corrompido no banco, quando verifica a senha, então devolve false e o aviso traz o id, sem o hash e sem a senha")
	void malformedHashDoesNotMatchAndTheWarningHidesSecrets(UnaryOperator<String> corrupt, CapturedOutput output) {
		// Arrange
		String password = TestPasswords.next();
		String hash = hasher.hash(password);
		String salt = hash.split("\\$")[4];
		String corrupted = corrupt.apply(hash);

		// Act
		boolean matches = hasher.matches(password, corrupted, 4321L);

		// Assert
		assertThat(matches).isFalse();
		assertThat(output.getAll()).contains("Malformed password hash; password rejected. Account id: 4321")
				.doesNotContain(password).doesNotContain("$argon2").doesNotContain(salt.substring(0, 8));
	}
}
