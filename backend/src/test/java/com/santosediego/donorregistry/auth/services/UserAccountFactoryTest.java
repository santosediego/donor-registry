package com.santosediego.donorregistry.auth.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.santosediego.donorregistry.auth.entities.UserAccount;
import com.santosediego.donorregistry.auth.services.exceptions.InvalidAccountDataException;
import com.santosediego.donorregistry.support.TestPasswords;

class UserAccountFactoryTest {

	private final PasswordHasher hasher = new PasswordHasher();
	private final UserAccountFactory factory = new UserAccountFactory(hasher);

	@ParameterizedTest(name = "[{0}]")
	@ValueSource(strings = { "admin.teste", "Admin.Teste", "ADMIN.TESTE" })
	@DisplayName("Dado o mesmo nome de usuário com caixa diferente, quando cria a conta, então o nome guardado é o mesmo")
	void usernameWithDifferentCaseIsTheSameUser(String typedUsername) {
		// Arrange
		String password = TestPasswords.next();

		// Act
		UserAccount account = factory.newAccount(typedUsername, password);

		// Assert
		assertThat(account.getUsername()).isEqualTo("admin.teste");
	}

	@ParameterizedTest(name = "[{0}]")
	@ValueSource(strings = { "curta-demais", " senha com espaço na ponta " })
	@DisplayName("Dado uma senha inválida, quando cria a conta, então o erro não repete a senha digitada")
	void errorNeverRepeatsThePassword(String password) {
		// Arrange
		String username = "admin.teste";

		// Act
		InvalidAccountDataException thrown = catchThrowableOfType(InvalidAccountDataException.class,
				() -> factory.newAccount(username, password));

		// Assert
		assertThat(thrown).isNotNull();
		assertThat(thrown.getMessage()).doesNotContain(password.strip()).doesNotContain(username);
		assertThat(thrown.getErrors()).isNotEmpty()
				.allSatisfy(error -> assertThat(error).doesNotContain(password.strip()).doesNotContain(username));
	}

	@Test
	@DisplayName("Dado uma senha igual ao nome de usuário, quando cria a conta, então recusa")
	void refusesPasswordEqualToTheUsername() {
		// Arrange
		String username = "administrador.teste";

		// Act
		InvalidAccountDataException thrown = catchThrowableOfType(InvalidAccountDataException.class,
				() -> factory.newAccount(username, username));

		// Assert
		assertThat(thrown).isNotNull();
		assertThat(thrown.getErrors()).containsExactly("A senha não pode ser igual ao nome de usuário.");
	}
}
