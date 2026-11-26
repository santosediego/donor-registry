package com.santosediego.donorregistry.auth;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Reads the source files: the auth package must not use anything else from this project. */
class AuthPackageIndependenceTest {

	private static final Path AUTH_SOURCES = Path.of("src/main/java/com/santosediego/donorregistry/auth");

	// Any project reference (import or full name) that does not continue with "auth"
	private static final Pattern OUTSIDE_AUTH = Pattern
			.compile("com\\.santosediego\\.donorregistry\\.(?!auth\\b)[\\w.*]+");

	@Test
	@DisplayName("Dado o código do pacote auth, quando procura referências ao projeto, então nenhuma aponta para fora de auth (nada de Donor ou Address)")
	void authDoesNotDependOnTheRestOfTheProject() throws IOException {
		// Arrange
		List<Path> sources;
		try (Stream<Path> files = Files.walk(AUTH_SOURCES)) {
			sources = files.filter(file -> file.toString().endsWith(".java")).toList();
		}
		List<String> violations = new ArrayList<>();

		// Act
		for (Path source : sources) {
			Matcher matcher = OUTSIDE_AUTH.matcher(Files.readString(source));
			while (matcher.find()) {
				violations.add(source.getFileName() + " -> " + matcher.group());
			}
		}

		// Assert
		assertThat(sources).as("source files found in %s", AUTH_SOURCES).isNotEmpty();
		assertThat(violations).isEmpty();
	}
}
