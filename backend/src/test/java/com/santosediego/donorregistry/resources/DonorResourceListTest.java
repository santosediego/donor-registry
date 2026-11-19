package com.santosediego.donorregistry.resources;

import static com.santosediego.donorregistry.support.ListAssertions.assertEveryItemHasExactlyTheItemFields;
import static com.santosediego.donorregistry.support.ListAssertions.assertItem;
import static com.santosediego.donorregistry.support.ListAssertions.assertPage;
import static com.santosediego.donorregistry.support.ListAssertions.names;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.jayway.jsonpath.JsonPath;
import com.santosediego.donorregistry.repositories.DonorRepository;
import com.santosediego.donorregistry.support.SeededListTestSupport;

import tools.jackson.databind.json.JsonMapper;

/**
 * GET /donors over the 16 seeds: response format and search. Read-only tests.
 * Ordering and pagination are in DonorResourceListPagingSortTest.
 */
class DonorResourceListTest extends SeededListTestSupport {

	// First 10 names of the default order (name ascending, as the H2 sorts: by character code).
	private static final List<String> FIRST_PAGE_NAMES = List.of("Alex James Murphy", "Christopher Pike",
			"James Tiberius Kirk", "Jean-Luc Picard", "Joao Cinco", "John Connor", "Johnny Five", "João Cinco",
			"Kathryn Janeway", "Leonard McCoy");

	@Autowired
	DonorResourceListTest(MockMvc mockMvc, DonorRepository repository, JsonMapper jsonMapper) {
		super(mockMvc, repository, jsonMapper);
	}

	@Test
	@DisplayName("LIST-1: Dado os 16 seeds, quando faço GET /donors sem parâmetros, então devolve 200 com 10 itens, página 0 de tamanho 10, total 16 e 2 páginas")
	void listsFirstPageWithDefaults() throws Exception {
		// Arrange: the 16 seeds (guarded by SeededListTestSupport)

		// Act
		ResultActions result = mockMvc.perform(get("/donors"));

		// Assert
		result.andExpect(status().isOk());
		assertThat(names(result)).containsExactlyElementsOf(FIRST_PAGE_NAMES);
		assertPage(result, 10, 0, 16, 2);
		assertThat(JsonPath.<Map<String, Object>>read(body(result), "$")).containsOnlyKeys("content", "page");
		assertEveryItemHasExactlyTheItemFields(result);
	}

	@Test
	@DisplayName("LIST-20: Dado um parâmetro inexistente (foo=bar), quando faço GET /donors, então devolve 200 e ignora o parâmetro (resposta igual à do LIST-1)")
	void ignoresUnknownParameter() throws Exception {
		// Arrange: the 16 seeds

		// Act
		ResultActions result = mockMvc.perform(get("/donors").param("foo", "bar"));

		// Assert
		result.andExpect(status().isOk());
		assertThat(names(result)).containsExactlyElementsOf(FIRST_PAGE_NAMES);
		assertPage(result, 10, 0, 16, 2);
	}

	@Test
	@DisplayName("LIST-10: Dado os seeds João Cinco e Joao Cinco, quando busco search=João, então devolve exatamente os 2, cada um igual ao seed (rg e city nulos onde o seed não tem)")
	void searchByNameWithAccentFindsBothJoaoCinco() throws Exception {
		// Arrange
		Long joaoWithAccentId = idOfCpf("00000050024");
		Long joaoPlainId = idOfCpf("00000050105");

		// Act
		ResultActions result = mockMvc.perform(get("/donors").param("search", "João"));

		// Assert: order by name in H2: "Joao" (plain) comes before "João" (accent)
		result.andExpect(status().isOk());
		assertPage(result, 10, 0, 2, 1);
		assertItem(result, 0, joaoPlainId, "Joao Cinco", "34900000109", "00000050105", "MG-00.000.501", "B-", null);
		assertItem(result, 1, joaoWithAccentId, "João Cinco", "34900000108", "00000050024", null, "A-",
				"Montes Claros");
	}

	@Test
	@DisplayName("LIST-11: Dado o termo sem acento no meio do nome, quando busco search=Cinco, então devolve os mesmos 2 itens do LIST-10")
	void searchByMiddleOfNameFindsBothJoaoCinco() throws Exception {
		// Arrange
		Long joaoWithAccentId = idOfCpf("00000050024");
		Long joaoPlainId = idOfCpf("00000050105");

		// Act
		ResultActions result = mockMvc.perform(get("/donors").param("search", "Cinco"));

		// Assert
		result.andExpect(status().isOk());
		assertPage(result, 10, 0, 2, 1);
		assertItem(result, 0, joaoPlainId, "Joao Cinco", "34900000109", "00000050105", "MG-00.000.501", "B-", null);
		assertItem(result, 1, joaoWithAccentId, "João Cinco", "34900000108", "00000050024", null, "A-",
				"Montes Claros");
	}

	@Test
	@DisplayName("LIST-12: Dado o seed Kathryn Janeway, quando busco search=Janeway, então devolve só ela, campo a campo, com total 1 e 1 página")
	void searchBySurnameFindsOneDonor() throws Exception {
		// Arrange
		Long id = idOfCpf("00000007447");

		// Act
		ResultActions result = mockMvc.perform(get("/donors").param("search", "Janeway"));

		// Assert
		result.andExpect(status().isOk());
		assertPage(result, 10, 0, 1, 1);
		assertItem(result, 0, id, "Kathryn Janeway", "34900000101", "00000007447", "MG-00.000.074", "O-",
				"Uberlândia");
	}

	@Test
	@DisplayName("LIST-13: Dado o seed Leonard McCoy (sem RG e sem endereço), quando busco pelo CPF 00000000272, então devolve só ele, com rg e city nulos")
	void searchByCpfFindsOneDonor() throws Exception {
		// Arrange
		Long id = idOfCpf("00000000272");

		// Act
		ResultActions result = mockMvc.perform(get("/donors").param("search", "00000000272"));

		// Assert
		result.andExpect(status().isOk());
		assertPage(result, 10, 0, 1, 1);
		assertItem(result, 0, id, "Leonard McCoy", "34900000103", "00000000272", null, "B+", null);
	}

	@Test
	@DisplayName("LIST-14: Dado o seed Christopher Pike, quando busco pelo RG MG-00.000.013, então devolve só ele")
	void searchByRgFindsOneDonor() throws Exception {
		// Arrange
		Long id = idOfCpf("00000001325");

		// Act
		ResultActions result = mockMvc.perform(get("/donors").param("search", "MG-00.000.013"));

		// Assert
		result.andExpect(status().isOk());
		assertPage(result, 10, 0, 1, 1);
		assertItem(result, 0, id, "Christopher Pike", "34900000111", "00000001325", "MG-00.000.013", "O+",
				"Araguari");
	}

	@Test
	@DisplayName("LIST-15: Dado o seed Christopher Pike, quando busco pelo RG sem pontuação MG00000013, então devolve só ele (o RG ignora pontuação)")
	void searchByRgIgnoresPunctuation() throws Exception {
		// Arrange
		Long id = idOfCpf("00000001325");

		// Act
		ResultActions result = mockMvc.perform(get("/donors").param("search", "MG00000013"));

		// Assert
		result.andExpect(status().isOk());
		assertPage(result, 10, 0, 1, 1);
		assertItem(result, 0, id, "Christopher Pike", "34900000111", "00000001325", "MG-00.000.013", "O+",
				"Araguari");
	}

	@Test
	@DisplayName("LIST-18b: Dado o termo J%o#a*o enviado já codificado (valor decodificado chega ao servidor), quando faço GET /donors, então devolve 200 sem resultados")
	void searchWithSpecialCharactersEncodedFindsNobody() throws Exception {
		// Arrange: MockMvc delivers the decoded value, so the % and # reach the search as plain text

		// Act
		ResultActions result = mockMvc.perform(get("/donors").param("search", "J%o#a*o"));

		// Assert
		result.andExpect(status().isOk());
		assertThat(names(result)).isEmpty();
		assertPage(result, 10, 0, 0, 0);
	}

	@Test
	@DisplayName("Extra: Dado search vazio, quando faço GET /donors, então devolve os 16 doadores (sem filtro)")
	void emptySearchReturnsEveryone() throws Exception {
		// Arrange: the 16 seeds

		// Act
		ResultActions result = mockMvc.perform(get("/donors").param("search", ""));

		// Assert
		result.andExpect(status().isOk());
		assertThat(names(result)).containsExactlyElementsOf(FIRST_PAGE_NAMES);
		assertPage(result, 10, 0, 16, 2);
	}

	@Test
	@DisplayName("Extra: Dado search só com espaços, quando faço GET /donors, então devolve os 16 doadores (sem filtro)")
	void blankSearchReturnsEveryone() throws Exception {
		// Arrange: the 16 seeds

		// Act
		ResultActions result = mockMvc.perform(get("/donors").param("search", "   "));

		// Assert
		result.andExpect(status().isOk());
		assertThat(names(result)).containsExactlyElementsOf(FIRST_PAGE_NAMES);
		assertPage(result, 10, 0, 16, 2);
	}

	@ParameterizedTest(name = "search={0}")
	@ValueSource(strings = { "joao", "JOAO", "João" })
	@DisplayName("S-2: Dado os seeds Joao Cinco e João Cinco, quando busco com minúsculas, maiúsculas ou acento, então o resultado é o mesmo (2 itens)")
	void searchIgnoresCaseAndAccent(String term) throws Exception {
		// Arrange: Joao Cinco and João Cinco are the only names with "joao" once accents and case are ignored

		// Act
		ResultActions result = mockMvc.perform(get("/donors").param("search", term));

		// Assert
		result.andExpect(status().isOk());
		assertThat(names(result)).containsExactly("Joao Cinco", "João Cinco");
		assertPage(result, 10, 0, 2, 1);
	}

	@Test
	@DisplayName("S-3: Dado um termo que ninguém tem (zzzz), quando busco, então devolve 200 com content vazio, total 0 e 0 páginas")
	void searchWithoutResultReturnsEmptyPage() throws Exception {
		// Arrange: no seed contains "zzzz"

		// Act
		ResultActions result = mockMvc.perform(get("/donors").param("search", "zzzz"));

		// Assert
		result.andExpect(status().isOk());
		assertThat(names(result)).isEmpty();
		assertPage(result, 10, 0, 0, 0);
	}

	@ParameterizedTest(name = "search={0}")
	@ValueSource(strings = { "%", "_" })
	@DisplayName("S-4: Dado % ou _ como termo (codificado), quando busco, então são literais e devolvem 0 resultados (não são coringas)")
	void likeWildcardsAreLiteral(String term) throws Exception {
		// Arrange: if % or _ were wildcards, the search would return (almost) every seed

		// Act
		ResultActions result = mockMvc.perform(get("/donors").param("search", term));

		// Assert
		result.andExpect(status().isOk());
		assertThat(names(result)).isEmpty();
		assertPage(result, 10, 0, 0, 0);
	}

	@Test
	@DisplayName("S-5: Dado search=Connor, sort=name,desc e size=1, quando faço GET /donors, então o total reflete o filtro (2) e ordem e paginação valem juntas (Sara Connor na página 0)")
	void searchSortAndPagingWorkTogether() throws Exception {
		// Arrange: two Connors: John and Sara; descending by name, Sara comes first

		// Act
		ResultActions result = mockMvc.perform(
				get("/donors").param("search", "Connor").param("sort", "name,desc").param("size", "1"));

		// Assert
		result.andExpect(status().isOk());
		assertThat(names(result)).containsExactly("Sara Connor");
		assertPage(result, 1, 0, 2, 2);
	}

	@Test
	@DisplayName("S-6: Dado o seed Alex James Murphy (RG MG-00.000.199), quando busco search=00.000.199, então devolve só ele")
	void searchByPartOfRgFindsOneDonor() throws Exception {
		// Arrange
		Long id = idOfCpf("00000198790");

		// Act
		ResultActions result = mockMvc.perform(get("/donors").param("search", "00.000.199"));

		// Assert
		result.andExpect(status().isOk());
		assertPage(result, 10, 0, 1, 1);
		assertItem(result, 0, id, "Alex James Murphy", "34900000107", "00000198790", "MG-00.000.199", "O+", "Uberaba");
	}

	private static String body(ResultActions result) throws Exception {
		return result.andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
	}
}
