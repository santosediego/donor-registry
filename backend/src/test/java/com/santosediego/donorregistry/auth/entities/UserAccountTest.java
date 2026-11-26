package com.santosediego.donorregistry.auth.entities;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.santosediego.donorregistry.auth.services.PasswordHasher;
import com.santosediego.donorregistry.support.TestPasswords;

class UserAccountTest {

	@Test
	@DisplayName("Dado uma conta com hash, quando converte para texto, então não aparecem o hash, a senha nem o nome de usuário")
	void textRepresentationHidesTheCredentials() {
		// Arrange
		String password = TestPasswords.next();
		String hash = new PasswordHasher().hash(password);
		UserAccount account = new UserAccount(7L, "admin.teste", hash, Instant.now(), null);

		// Act
		String text = account.toString();

		// Assert
		assertThat(text).contains("7").doesNotContain(hash).doesNotContain("$argon2").doesNotContain(password)
				.doesNotContain("admin.teste");
	}
}
