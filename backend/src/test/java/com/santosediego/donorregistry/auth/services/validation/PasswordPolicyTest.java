package com.santosediego.donorregistry.auth.services.validation;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class PasswordPolicyTest {

	private static final String USERNAME = "administrador.teste";

	private static final String TOO_SHORT = "A senha deve ter pelo menos 15 caracteres. Use uma frase: várias "
			+ "palavras separadas por espaço são fáceis de lembrar.";
	private static final String TOO_LONG = "A senha deve ter no máximo 128 caracteres.";
	private static final String SURROUNDING_SPACE = "A senha não pode começar nem terminar com espaço.";
	private static final String SAME_AS_USERNAME = "A senha não pode ser igual ao nome de usuário.";

	@ParameterizedTest(name = "[{0}]")
	@NullAndEmptySource
	@DisplayName("Dado uma senha nula ou vazia, quando valida, então recusa pedindo a senha, sem NullPointerException")
	void rejectsMissingPassword(String password) {
		// Arrange (password comes from the source)

		// Act
		List<String> errors = PasswordPolicy.validate(password, USERNAME);

		// Assert
		assertThat(errors).containsExactly("A senha é obrigatória.");
	}

	@ParameterizedTest(name = "[{0}]")
	@ValueSource(strings = { "   ", "                    ", "\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t" })
	@DisplayName("Dado uma senha só com espaços, quando valida, então recusa")
	void rejectsBlankPassword(String password) {
		// Arrange (password comes from the source)

		// Act
		List<String> errors = PasswordPolicy.validate(password, USERNAME);

		// Assert
		assertThat(errors).contains(SURROUNDING_SPACE);
	}

	@Test
	@DisplayName("Dado uma senha com 14 caracteres, quando valida, então recusa sugerindo usar uma frase")
	void rejectsPasswordShorterThanTheMinimum() {
		// Arrange
		String password = passwordOfLength(14);

		// Act
		List<String> errors = PasswordPolicy.validate(password, USERNAME);

		// Assert
		assertThat(errors).containsExactly(TOO_SHORT);
	}

	@ParameterizedTest(name = "{0} caracteres")
	@ValueSource(ints = { 15, 128 })
	@DisplayName("Dado uma senha no limite mínimo ou máximo, quando valida, então aceita")
	void acceptsPasswordAtTheLimits(int length) {
		// Arrange
		String password = passwordOfLength(length);

		// Act
		List<String> errors = PasswordPolicy.validate(password, USERNAME);

		// Assert
		assertThat(errors).isEmpty();
	}

	@Test
	@DisplayName("Dado uma senha com 129 caracteres, quando valida, então recusa pelo máximo")
	void rejectsPasswordLongerThanTheMaximum() {
		// Arrange
		String password = passwordOfLength(129);

		// Act
		List<String> errors = PasswordPolicy.validate(password, USERNAME);

		// Assert
		assertThat(errors).containsExactly(TOO_LONG);
	}

	@ParameterizedTest(name = "[{0}]")
	@ValueSource(strings = { " frase de teste com espaço", "frase de teste com espaço ", " frase de teste com espaço" })
	@DisplayName("Dado uma senha com espaço em uma das pontas, quando valida, então recusa com mensagem clara (não corta em silêncio)")
	void rejectsSurroundingSpaces(String password) {
		// Arrange (password comes from the source)

		// Act
		List<String> errors = PasswordPolicy.validate(password, USERNAME);

		// Assert
		assertThat(errors).containsExactly(SURROUNDING_SPACE);
	}

	@ParameterizedTest(name = "[{0}]")
	@ValueSource(strings = { "administrador.teste", "Administrador.Teste", "ADMINISTRADOR.TESTE" })
	@DisplayName("Dado uma senha igual ao nome de usuário, em qualquer caixa, quando valida, então recusa")
	void rejectsPasswordEqualToTheUsername(String password) {
		// Arrange (password comes from the source)

		// Act
		List<String> errors = PasswordPolicy.validate(password, USERNAME);

		// Assert
		assertThat(errors).containsExactly(SAME_AS_USERNAME);
	}

	// Alternates two letters, so it never trips the "single repeated character" rule
	private static String passwordOfLength(int length) {
		return "ab".repeat(length).substring(0, length);
	}
}
