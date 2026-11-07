package com.santosediego.donorregistry.repositories;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DonorSearchQueriesRgPatternTest {

	@Test
	@DisplayName("Dado um RG com UF, pontos e hífen, quando monta o padrão do RG, então remove a pontuação e põe em minúsculas")
	void removesPunctuationAndLowersCase() {
		// Arrange
		String term = "MG-00.000.199";

		// Act
		String pattern = DonorSearchQueries.rgPattern(term);

		// Assert
		assertThat(pattern).isEqualTo("%mg00000199%");
	}

	@Test
	@DisplayName("Dado um RG só com números e pontos, quando monta o padrão do RG, então remove os pontos")
	void removesDotsFromNumericRg() {
		// Arrange
		String term = "00.000.199";

		// Act
		String pattern = DonorSearchQueries.rgPattern(term);

		// Assert
		assertThat(pattern).isEqualTo("%00000199%");
	}

	@Test
	@DisplayName("Dado um termo só de pontuação, quando monta o padrão do RG, então devolve vazio e nunca %%, que casaria com todos")
	void returnsEmptyWhenOnlyPunctuation() {
		// Arrange
		String term = "-";

		// Act
		String pattern = DonorSearchQueries.rgPattern(term);

		// Assert
		assertThat(pattern).isEmpty();
	}
}
