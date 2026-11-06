package com.santosediego.donorregistry.repositories;

import static org.assertj.core.api.Assertions.assertThat;

import java.text.Normalizer;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DonorSearchQueriesTranslateListsTest {

	@Test
	@DisplayName("Dado as listas ACCENTED e PLAIN do TRANSLATE, quando compara o tamanho, então são iguais (senão o TRANSLATE remove letras sem avisar)")
	void accentedAndPlainHaveTheSameLength() {
		// Arrange
		String accented = DonorSearchQueries.ACCENTED;
		String plain = DonorSearchQueries.PLAIN;

		// Act
		int accentedLength = accented.length();
		int plainLength = plain.length();

		// Assert
		assertThat(accentedLength).isEqualTo(plainLength);
	}

	@Test
	@DisplayName("Dado as listas ACCENTED e PLAIN, quando tira o acento de cada letra de ACCENTED, então dá a letra da mesma posição em PLAIN")
	void eachAccentedLetterMapsToItsPlainLetterAtTheSamePosition() {
		// Arrange
		String accented = DonorSearchQueries.ACCENTED;
		String plain = DonorSearchQueries.PLAIN;

		for (int i = 0; i < accented.length(); i++) {
			// Act
			String withoutAccent = Normalizer.normalize(String.valueOf(accented.charAt(i)), Normalizer.Form.NFD)
					.replaceAll("\\p{M}", "");

			// Assert
			assertThat(withoutAccent)
					.as("posição %d: '%s' deveria virar '%s'", i, accented.charAt(i), plain.charAt(i))
					.isEqualTo(String.valueOf(plain.charAt(i)));
		}
	}
}
