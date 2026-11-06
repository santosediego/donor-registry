package com.santosediego.donorregistry.repositories;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DonorSearchQueriesLikePatternTest {

	@Test
	@DisplayName("Dado um termo simples, quando monta o padrão LIKE, então cerca o termo com %")
	void wrapsPlainTermWithWildcards() {
		// Arrange
		String term = "joao";

		// Act
		String pattern = DonorSearchQueries.likePattern(term);

		// Assert
		assertThat(pattern).isEqualTo("%joao%");
	}

	@Test
	@DisplayName("Dado um termo com %, quando monta o padrão LIKE, então escapa o % com ! para ele valer como letra")
	void escapesPercentSign() {
		// Arrange
		String term = "50%";

		// Act
		String pattern = DonorSearchQueries.likePattern(term);

		// Assert
		assertThat(pattern).isEqualTo("%50!%%");
	}

	@Test
	@DisplayName("Dado um termo com _, quando monta o padrão LIKE, então escapa o _ com ! para ele valer como letra")
	void escapesUnderscore() {
		// Arrange
		String term = "a_b";

		// Act
		String pattern = DonorSearchQueries.likePattern(term);

		// Assert
		assertThat(pattern).isEqualTo("%a!_b%");
	}

	@Test
	@DisplayName("Dado um termo com o próprio caractere de escape !, quando monta o padrão LIKE, então dobra o !")
	void escapesTheEscapeCharacterItself() {
		// Arrange
		String term = "!";

		// Act
		String pattern = DonorSearchQueries.likePattern(term);

		// Assert
		assertThat(pattern).isEqualTo("%!!%");
	}

	@Test
	@DisplayName("Dado um termo com ! seguido de %, quando monta o padrão LIKE, então escapa os dois sem confundir um com o outro")
	void escapesEscapeCharacterBeforePercentSign() {
		// Arrange
		String term = "!%";

		// Act
		String pattern = DonorSearchQueries.likePattern(term);

		// Assert
		assertThat(pattern).isEqualTo("%!!!%%");
	}

	@Test
	@DisplayName("Dado um termo com espaços nas pontas, quando monta o padrão LIKE, então mantém os espaços (tirar espaços é do service)")
	void keepsSurroundingSpaces() {
		// Arrange
		String term = " joao ";

		// Act
		String pattern = DonorSearchQueries.likePattern(term);

		// Assert
		assertThat(pattern).isEqualTo("% joao %");
	}

	@Test
	@DisplayName("Dado um termo em maiúsculas e com acento, quando monta o padrão LIKE, então não muda maiúscula nem acento (isso é do SQL)")
	void keepsCaseAndAccents() {
		// Arrange
		String term = "JOÃO";

		// Act
		String pattern = DonorSearchQueries.likePattern(term);

		// Assert
		assertThat(pattern).isEqualTo("%JOÃO%");
	}
}
