package com.santosediego.donorregistry.resources;

import static com.santosediego.donorregistry.support.DonorRequests.fullBody;
import static com.santosediego.donorregistry.support.DonorRequests.minimalBody;
import static com.santosediego.donorregistry.support.ErrorAssertions.assertFieldErrors;
import static com.santosediego.donorregistry.support.ErrorAssertions.assertStandardError;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.jayway.jsonpath.JsonPath;
import com.santosediego.donorregistry.entities.Donor;
import com.santosediego.donorregistry.repositories.DonorRepository;
import com.santosediego.donorregistry.support.ApiTestSupport;
import com.santosediego.donorregistry.support.TestCpf;

import tools.jackson.databind.json.JsonMapper;

class DonorResourcePostTest extends ApiTestSupport {

	@Autowired
	DonorResourcePostTest(MockMvc mockMvc, DonorRepository repository, JsonMapper jsonMapper) {
		super(mockMvc, repository, jsonMapper);
	}

	@Test
	@DisplayName("Dado um corpo completo, com endereço, quando faço POST /donors, então cria o doador com 201 e grava doador e endereço ligados")
	void createsDonorWithAddress() throws Exception {
		// Arrange
		Map<String, Object> body = fullBody();
		long countBefore = repository.count();
		Instant before = Instant.now();

		// Act
		ResultActions result = mockMvc.perform(post("/donors").contentType(MediaType.APPLICATION_JSON)
				.content(json(body)));
		Instant after = Instant.now();

		// Assert: response
		result.andExpect(status().isCreated())
				.andExpect(jsonPath("$.id").value(notNullValue()))
				.andExpect(jsonPath("$.name").value("Dana Scully"))
				.andExpect(jsonPath("$.cpf").value(body.get("cpf")))
				.andExpect(jsonPath("$.rg").value("MG-12.345.678"))
				.andExpect(jsonPath("$.birthDate").value("1990-05-20"))
				.andExpect(jsonPath("$.gender").value("F"))
				.andExpect(jsonPath("$.maritalStatus").value("SOL"))
				.andExpect(jsonPath("$.bloodType").value("A+"))
				.andExpect(jsonPath("$.phone").value("11912345678"))
				.andExpect(jsonPath("$.weight").value(70.5))
				.andExpect(jsonPath("$.address.id").value(notNullValue()))
				.andExpect(jsonPath("$.address.street").value("Rua das Flores"))
				.andExpect(jsonPath("$.address.number").value("123"))
				.andExpect(jsonPath("$.address.complement").value("Apto 4"))
				.andExpect(jsonPath("$.address.neighborhood").value("Centro"))
				.andExpect(jsonPath("$.address.zipCode").value("01310100"))
				.andExpect(jsonPath("$.address.city").value("São Paulo"))
				.andExpect(jsonPath("$.address.state").value("SP"))
				.andExpect(jsonPath("$.updatedAt").value(nullValue()));
		String responseBody = result.andReturn().getResponse().getContentAsString();
		Number id = JsonPath.read(responseBody, "$.id");
		result.andExpect(header().string("Location", "http://localhost/donors/" + id));
		Instant createdAt = Instant.parse(JsonPath.read(responseBody, "$.createdAt"));
		assertThat(createdAt).isBetween(before, after);

		// Assert: database
		assertThat(repository.count()).isEqualTo(countBefore + 1);
		Donor saved = repository.findById(id.longValue()).orElseThrow();
		assertThat(saved.getCpf()).isEqualTo(body.get("cpf"));
		assertThat(saved.getAddress()).isNotNull();
		assertThat(saved.getAddress().getDonor().getId()).isEqualTo(saved.getId());
		assertThat(saved.getAddress().getCity()).isEqualTo("São Paulo");
	}

	@Test
	@DisplayName("Dado um corpo só com os obrigatórios, sem endereço, quando faço POST /donors, então cria com 201 e os opcionais ficam nulos")
	void createsDonorWithRequiredFieldsOnly() throws Exception {
		// Arrange
		Map<String, Object> body = minimalBody();
		long countBefore = repository.count();
		Instant before = Instant.now();

		// Act
		ResultActions result = mockMvc.perform(post("/donors").contentType(MediaType.APPLICATION_JSON)
				.content(json(body)));
		Instant after = Instant.now();

		// Assert: response
		result.andExpect(status().isCreated())
				.andExpect(jsonPath("$.id").value(notNullValue()))
				.andExpect(jsonPath("$.name").value("Dana Scully"))
				.andExpect(jsonPath("$.cpf").value(body.get("cpf")))
				.andExpect(jsonPath("$.birthDate").value("1990-05-20"))
				.andExpect(jsonPath("$.gender").value("F"))
				.andExpect(jsonPath("$.phone").value("11912345678"))
				.andExpect(jsonPath("$.rg").value(nullValue()))
				.andExpect(jsonPath("$.maritalStatus").value(nullValue()))
				.andExpect(jsonPath("$.bloodType").value(nullValue()))
				.andExpect(jsonPath("$.weight").value(nullValue()))
				.andExpect(jsonPath("$.address").value(nullValue()));
		String responseBody = result.andReturn().getResponse().getContentAsString();
		Instant createdAt = Instant.parse(JsonPath.read(responseBody, "$.createdAt"));
		assertThat(createdAt).isBetween(before, after);

		// Assert: database
		assertThat(repository.count()).isEqualTo(countBefore + 1);
		Number id = JsonPath.read(responseBody, "$.id");
		Donor saved = repository.findById(id.longValue()).orElseThrow();
		assertThat(saved.getAddress()).isNull();
		assertThat(saved.getWeight()).isNull();
	}

	@Test
	@DisplayName("Dado o CPF 12345678900 (dígitos verificadores errados), quando faço POST /donors, então devolve 400 com o erro no campo cpf e não grava")
	void rejectsCpfWithInvalidCheckDigits() throws Exception {
		// Arrange
		Map<String, Object> body = minimalBody();
		body.put("cpf", "12345678900");
		long countBefore = repository.count();
		Instant before = Instant.now();

		// Act
		ResultActions result = mockMvc.perform(post("/donors").contentType(MediaType.APPLICATION_JSON)
				.content(json(body)));
		Instant after = Instant.now();

		// Assert
		assertStandardError(result, 400, "Dados inválidos", "Há campos com valores inválidos.", "/donors", before,
				after);
		assertFieldErrors(result, Map.of("cpf", "CPF inválido."));
		assertThat(repository.count()).isEqualTo(countBefore);
	}

	@Test
	@DisplayName("Dado o CPF mascarado 123.456.789-09, quando faço POST /donors, então devolve 400 pedindo 11 números e não grava")
	void rejectsMaskedCpf() throws Exception {
		// Arrange
		Map<String, Object> body = minimalBody();
		body.put("cpf", "123.456.789-09");
		long countBefore = repository.count();
		Instant before = Instant.now();

		// Act
		ResultActions result = mockMvc.perform(post("/donors").contentType(MediaType.APPLICATION_JSON)
				.content(json(body)));
		Instant after = Instant.now();

		// Assert
		assertStandardError(result, 400, "Dados inválidos", "Há campos com valores inválidos.", "/donors", before,
				after);
		assertFieldErrors(result, Map.of("cpf", "O CPF deve conter 11 números."));
		assertThat(repository.count()).isEqualTo(countBefore);
	}

	@Test
	@DisplayName("Dado o CPF de um doador que já existe, quando faço POST /donors, então devolve 409 sem dados do existente e continua só um doador com o CPF")
	void rejectsDuplicateCpf() throws Exception {
		// Arrange
		String cpf = TestCpf.next();
		repository.save(newDonor(cpf, "Existing Donor Name"));
		Map<String, Object> body = minimalBody();
		body.put("cpf", cpf);
		long countBefore = repository.count();
		Instant before = Instant.now();

		// Act
		ResultActions result = mockMvc.perform(post("/donors").contentType(MediaType.APPLICATION_JSON)
				.content(json(body)));
		Instant after = Instant.now();

		// Assert
		assertStandardError(result, 409, "Conflito", "Já existe um doador cadastrado com este CPF.", "/donors",
				before, after);
		String responseBody = result.andReturn().getResponse().getContentAsString();
		assertThat(responseBody).doesNotContain("Existing Donor Name").doesNotContain(cpf)
				.doesNotContain("21987654321");
		assertThat(repository.count()).isEqualTo(countBefore);
		assertThat(repository.findAll().stream().filter(d -> cpf.equals(d.getCpf()))).hasSize(1);
	}

	@Test
	@DisplayName("Dado gender X, quando faço POST /donors, então devolve 400 listando os valores aceitos e não grava")
	void rejectsUnknownGender() throws Exception {
		// Arrange
		Map<String, Object> body = minimalBody();
		body.put("gender", "X");
		long countBefore = repository.count();
		Instant before = Instant.now();

		// Act
		ResultActions result = mockMvc.perform(post("/donors").contentType(MediaType.APPLICATION_JSON)
				.content(json(body)));
		Instant after = Instant.now();

		// Assert
		assertStandardError(result, 400, "Requisição inválida",
				"Valor inválido para gênero: 'X'. Valores aceitos: M, F, OT.", "/donors", before, after);
		assertThat(repository.count()).isEqualTo(countBefore);
	}

	@Test
	@DisplayName("Dado address vazio ({}), quando faço POST /donors, então devolve 400 com os seis campos obrigatórios do endereço e não grava")
	void rejectsEmptyAddress() throws Exception {
		// Arrange
		Map<String, Object> body = minimalBody();
		body.put("address", Map.of());
		long countBefore = repository.count();
		Instant before = Instant.now();

		// Act
		ResultActions result = mockMvc.perform(post("/donors").contentType(MediaType.APPLICATION_JSON)
				.content(json(body)));
		Instant after = Instant.now();

		// Assert
		assertStandardError(result, 400, "Dados inválidos", "Há campos com valores inválidos.", "/donors", before,
				after);
		assertFieldErrors(result, Map.of(
				"address.street", "A rua é obrigatória.",
				"address.number", "O número é obrigatório.",
				"address.neighborhood", "O bairro é obrigatório.",
				"address.zipCode", "O CEP é obrigatório.",
				"address.city", "A cidade é obrigatória.",
				"address.state", "O estado é obrigatório."));
		assertThat(repository.count()).isEqualTo(countBefore);
	}

	@Test
	@DisplayName("Dado um JSON quebrado, quando faço POST /donors, então devolve 400 de corpo inválido e não grava")
	void rejectsMalformedJson() throws Exception {
		// Arrange
		String brokenJson = "{ \"name\": \"Dana Scully\", ";
		long countBefore = repository.count();
		Instant before = Instant.now();

		// Act
		ResultActions result = mockMvc.perform(post("/donors").contentType(MediaType.APPLICATION_JSON)
				.content(brokenJson));
		Instant after = Instant.now();

		// Assert
		assertStandardError(result, 400, "Requisição inválida",
				"Corpo da requisição inválido. Verifique o JSON e as datas enviadas.", "/donors", before, after);
		assertThat(repository.count()).isEqualTo(countBefore);
	}

	@Test
	@DisplayName("Dado uma birthDate inexistente (2030-13-45), quando faço POST /donors, então cai no tratamento de corpo inválido (400) e não grava")
	void rejectsUnparseableBirthDate() throws Exception {
		// Arrange
		Map<String, Object> body = minimalBody();
		body.put("birthDate", "2030-13-45");
		long countBefore = repository.count();
		Instant before = Instant.now();

		// Act
		ResultActions result = mockMvc.perform(post("/donors").contentType(MediaType.APPLICATION_JSON)
				.content(json(body)));
		Instant after = Instant.now();

		// Assert
		assertStandardError(result, 400, "Requisição inválida",
				"Corpo da requisição inválido. Verifique o JSON e as datas enviadas.", "/donors", before, after);
		assertThat(repository.count()).isEqualTo(countBefore);
	}

	@Test
	@DisplayName("Dado uma birthDate no futuro, quando faço POST /donors, então devolve 400 de validação no campo birthDate e não grava")
	void rejectsFutureBirthDate() throws Exception {
		// Arrange
		Map<String, Object> body = minimalBody();
		body.put("birthDate", "2999-01-01");
		long countBefore = repository.count();
		Instant before = Instant.now();

		// Act
		ResultActions result = mockMvc.perform(post("/donors").contentType(MediaType.APPLICATION_JSON)
				.content(json(body)));
		Instant after = Instant.now();

		// Assert
		assertStandardError(result, 400, "Dados inválidos", "Há campos com valores inválidos.", "/donors", before,
				after);
		assertFieldErrors(result, Map.of("birthDate", "A data de nascimento deve estar no passado."));
		assertThat(repository.count()).isEqualTo(countBefore);
	}
}
