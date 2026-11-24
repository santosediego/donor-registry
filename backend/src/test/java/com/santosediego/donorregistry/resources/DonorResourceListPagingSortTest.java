package com.santosediego.donorregistry.resources;

import static com.santosediego.donorregistry.support.ErrorAssertions.assertStandardError;
import static com.santosediego.donorregistry.support.ListAssertions.assertPage;
import static com.santosediego.donorregistry.support.ListAssertions.names;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.santosediego.donorregistry.repositories.DonorRepository;
import com.santosediego.donorregistry.support.SeededListTestSupport;

import tools.jackson.databind.json.JsonMapper;

/** GET /donors over the 16 seeds: pagination, limits and sorting. Read-only tests. */
class DonorResourceListPagingSortTest extends SeededListTestSupport {

	private static final String INVALID_SORT_MESSAGE = "Campo de ordenação inválido. Campos aceitos: name, createdAt. Use, por exemplo, sort=name,asc.";

	/*
	 * Name ascending, as the H2 sorts: by character code, not by dictionary order. So "João Cinco"
	 * (with the accent, ó is after every ASCII letter) comes AFTER "Johnny Five", while "Joao Cinco" comes
	 * before "John Connor". This proves the H2 order only; PostgreSQL may differ (collation), see ROADMAP 5.3.
	 */
	private static final List<String> NAME_ASC = List.of("Alex James Murphy", "Christopher Pike",
			"James Tiberius Kirk", "Jean-Luc Picard", "Joao Cinco", "John Connor", "Johnny Five", "João Cinco",
			"Kathryn Janeway", "Leonard McCoy", "Lion-O", "Montgomery Scott", "Nyota Uhura", "Sara Connor", "Spock",
			"Wall-E");

	// Registration order: the seeds have created_at from 2026-01-15 to 2026-01-30, one per day, in this order
	private static final List<String> CREATED_AT_ASC = List.of("Kathryn Janeway", "Jean-Luc Picard",
			"Leonard McCoy", "Spock", "Sara Connor", "John Connor", "Alex James Murphy", "João Cinco", "Joao Cinco",
			"James Tiberius Kirk", "Christopher Pike", "Nyota Uhura", "Montgomery Scott", "Lion-O", "Wall-E",
			"Johnny Five");

	@Autowired
	DonorResourceListPagingSortTest(MockMvc mockMvc, DonorRepository repository, JsonMapper jsonMapper) {
		super(mockMvc, repository, jsonMapper);
	}

	private static List<String> reversed(List<String> list) {
		List<String> copy = new ArrayList<>(list);
		Collections.reverse(copy);
		return copy;
	}

	@Test
	@DisplayName("LIST-2: Dado os 16 seeds, quando faço GET /donors?size=5, então devolve 5 itens, page.size 5 e 4 páginas")
	void sizeFiveReturnsFiveItems() throws Exception {
		// Arrange: the 16 seeds

		// Act
		ResultActions result = mockMvc.perform(get("/donors").param("size", "5"));

		// Assert
		result.andExpect(status().isOk());
		assertThat(names(result)).containsExactlyElementsOf(NAME_ASC.subList(0, 5));
		assertPage(result, 5, 0, 16, 4);
	}

	@Test
	@DisplayName("LIST-3: Dado os 16 seeds, quando faço GET /donors?page=1, então devolve os 6 itens restantes e page.number 1")
	void secondPageReturnsRemainingItems() throws Exception {
		// Arrange: 16 seeds, 10 per page, so page 1 has 6

		// Act
		ResultActions result = mockMvc.perform(get("/donors").param("page", "1"));

		// Assert
		result.andExpect(status().isOk());
		assertThat(names(result)).containsExactlyElementsOf(NAME_ASC.subList(10, 16));
		assertPage(result, 10, 1, 16, 2);
	}

	@Test
	@DisplayName("S-1: Dado os 16 seeds, quando faço GET /donors sem sort, então os itens vêm em ordem crescente de name (ordenação padrão)")
	void defaultOrderIsNameAscending() throws Exception {
		// Arrange: size=100 so the whole list is in one page; no sort on purpose

		// Act
		ResultActions result = mockMvc.perform(get("/donors").param("size", "100"));

		// Assert
		result.andExpect(status().isOk());
		assertThat(names(result)).containsExactlyElementsOf(NAME_ASC);
	}

	@Test
	@DisplayName("LIST-4: Dado os 16 seeds, quando faço GET /donors?sort=name, então os itens vêm em ordem crescente de name")
	void sortByNameAscending() throws Exception {
		// Arrange: size=100 so the whole list is in one page

		// Act
		ResultActions result = mockMvc.perform(get("/donors").param("sort", "name").param("size", "100"));

		// Assert
		result.andExpect(status().isOk());
		assertThat(names(result)).containsExactlyElementsOf(NAME_ASC);
	}

	@Test
	@DisplayName("LIST-5: Dado os 16 seeds, quando faço GET /donors?sort=name,desc, então os itens vêm em ordem decrescente de name")
	void sortByNameDescending() throws Exception {
		// Arrange: size=100 so the whole list is in one page

		// Act
		ResultActions result = mockMvc.perform(get("/donors").param("sort", "name,desc").param("size", "100"));

		// Assert
		result.andExpect(status().isOk());
		assertThat(names(result)).containsExactlyElementsOf(reversed(NAME_ASC));
	}

	@Test
	@DisplayName("LIST-6: Dado os 16 seeds, quando faço GET /donors?sort=createdAt, então os itens vêm em ordem crescente de cadastro")
	void sortByCreatedAtAscending() throws Exception {
		// Arrange: size=100 so the whole list is in one page

		// Act
		ResultActions result = mockMvc.perform(get("/donors").param("sort", "createdAt").param("size", "100"));

		// Assert
		result.andExpect(status().isOk());
		assertThat(names(result)).containsExactlyElementsOf(CREATED_AT_ASC);
	}

	@Test
	@DisplayName("LIST-7: Dado os 16 seeds, quando faço GET /donors?sort=createdAt,desc, então os itens vêm em ordem decrescente de cadastro")
	void sortByCreatedAtDescending() throws Exception {
		// Arrange: size=100 so the whole list is in one page

		// Act
		ResultActions result = mockMvc.perform(get("/donors").param("sort", "createdAt,desc").param("size", "100"));

		// Assert
		result.andExpect(status().isOk());
		assertThat(names(result)).containsExactlyElementsOf(reversed(CREATED_AT_ASC));
	}

	@Test
	@DisplayName("LIST-8: Dado os 16 seeds, quando faço GET /donors?page=999, então devolve 200 com content vazio, page.number 999 e total 16")
	void pageBeyondTheEndReturnsEmptyContent() throws Exception {
		// Arrange: only 2 pages exist

		// Act
		ResultActions result = mockMvc.perform(get("/donors").param("page", "999"));

		// Assert
		result.andExpect(status().isOk());
		assertThat(names(result)).isEmpty();
		assertPage(result, 10, 999, 16, 2);
	}

	@Test
	@DisplayName("LIST-9: Dado os 16 seeds, quando faço GET /donors?size=900, então page.size é limitado a 100")
	void sizeAboveTheLimitIsReducedToOneHundred() throws Exception {
		// Arrange: the limit is spring.data.web.pageable.max-page-size=100

		// Act
		ResultActions result = mockMvc.perform(get("/donors").param("size", "900"));

		// Assert
		result.andExpect(status().isOk());
		assertThat(names(result)).hasSize(16);
		assertPage(result, 100, 0, 16, 1);
	}

	@Test
	@DisplayName("LIST-16: Dado um campo de ordenação inexistente, quando faço GET /donors?sort=test, então devolve 400 com a mensagem de campo de ordenação inválido")
	void unknownSortFieldReturnsBadRequest() throws Exception {
		// Arrange
		Instant before = Instant.now();

		// Act
		ResultActions result = mockMvc.perform(get("/donors").param("sort", "test"));

		// Assert
		assertStandardError(result, 400, "Requisição inválida", INVALID_SORT_MESSAGE, "/donors", before,
				Instant.now());
	}

	@Test
	@DisplayName("Extra: Dado uma direção inválida, quando faço GET /donors?sort=name,xyz, então devolve 400 com a mensagem do LIST-16")
	void invalidSortDirectionReturnsBadRequest() throws Exception {
		// Arrange: "xyz" is not a direction, so Spring reads it as a field name, which is not accepted
		Instant before = Instant.now();

		// Act
		ResultActions result = mockMvc.perform(get("/donors").param("sort", "name,xyz"));

		// Assert
		assertStandardError(result, 400, "Requisição inválida", INVALID_SORT_MESSAGE, "/donors", before,
				Instant.now());
	}

	@Test
	@DisplayName("S-7: Dado dois sort, o primeiro válido e o segundo inválido, quando faço GET /donors?sort=name&sort=xyz, então devolve 400 (todas as ordenações são validadas)")
	void invalidSecondSortReturnsBadRequest() throws Exception {
		// Arrange: the first sort is valid on purpose; only the second one is wrong
		Instant before = Instant.now();

		// Act
		ResultActions result = mockMvc.perform(get("/donors").param("sort", "name", "xyz"));

		// Assert
		assertStandardError(result, 400, "Requisição inválida", INVALID_SORT_MESSAGE, "/donors", before,
				Instant.now());
	}

	@ParameterizedTest(name = "page={0}")
	@ValueSource(strings = { "-1", "abc" })
	@DisplayName("LIST-17: Dado page inválida (negativa ou texto), quando faço GET /donors, então devolve 200 tratada como página 0 (mesmos itens do LIST-1)")
	void invalidPageFallsBackToFirstPage(String page) throws Exception {
		// Arrange: the 16 seeds

		// Act
		ResultActions result = mockMvc.perform(get("/donors").param("page", page));

		// Assert
		result.andExpect(status().isOk());
		assertThat(names(result)).containsExactlyElementsOf(NAME_ASC.subList(0, 10));
		assertPage(result, 10, 0, 16, 2);
	}

	@ParameterizedTest(name = "size={0}")
	@ValueSource(strings = { "-1", "0", "abc" })
	@DisplayName("LIST-19: Dado size inválido (negativo, zero ou texto), quando faço GET /donors, então devolve 200 com o tamanho padrão (page.size 10)")
	void invalidSizeFallsBackToDefault(String size) throws Exception {
		// Arrange: the 16 seeds

		// Act
		ResultActions result = mockMvc.perform(get("/donors").param("size", size));

		// Assert
		result.andExpect(status().isOk());
		assertThat(names(result)).containsExactlyElementsOf(NAME_ASC.subList(0, 10));
		assertPage(result, 10, 0, 16, 2);
	}
}
