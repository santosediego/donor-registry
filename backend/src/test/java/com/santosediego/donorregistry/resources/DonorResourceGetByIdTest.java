package com.santosediego.donorregistry.resources;

import static com.santosediego.donorregistry.support.ErrorAssertions.assertStandardError;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.jayway.jsonpath.JsonPath;
import com.santosediego.donorregistry.entities.Address;
import com.santosediego.donorregistry.entities.Donor;
import com.santosediego.donorregistry.repositories.DonorRepository;
import com.santosediego.donorregistry.support.ApiTestSupport;
import com.santosediego.donorregistry.support.TestCpf;

import tools.jackson.databind.json.JsonMapper;

class DonorResourceGetByIdTest extends ApiTestSupport {

	@Autowired
	DonorResourceGetByIdTest(MockMvc mockMvc, DonorRepository repository, JsonMapper jsonMapper) {
		super(mockMvc, repository, jsonMapper);
	}

	@Test
	@DisplayName("Dado um doador já gravado, quando faço GET /donors/{id}, então devolve 200 com cada campo igual ao gravado")
	void returnsStoredDonor() throws Exception {
		// Arrange: created straight by the repository, so this test does not depend on the POST
		String cpf = TestCpf.next();
		Donor donor = new Donor();
		donor.setName("Ellen Ripley");
		donor.setCpf(cpf);
		donor.setRg("SP-11.222.333");
		donor.setBirthDate(LocalDate.of(1979, 6, 22));
		donor.setGender("F");
		donor.setMaritalStatus("DIV");
		donor.setBloodType("O-");
		donor.setPhone("11955554444");
		donor.setWeight(new BigDecimal("62.5"));
		donor.setUpdatedAt(Instant.parse("2025-01-02T03:04:05Z"));
		Address address = new Address();
		address.setStreet("Avenida Nostromo");
		address.setNumber("S/N");
		address.setComplement("Bloco B");
		address.setNeighborhood("Bairro Alto");
		address.setZipCode("30140071");
		address.setCity("Belo Horizonte");
		address.setState("MG");
		donor.setAddress(address);
		repository.save(donor);
		// read back what the database really holds (microseconds), to compare the dates
		Donor stored = repository.findById(donor.getId()).orElseThrow();
		long countBefore = repository.count();

		// Act
		ResultActions result = mockMvc.perform(get("/donors/{id}", stored.getId()));

		// Assert
		result.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(stored.getId()))
				.andExpect(jsonPath("$.name").value("Ellen Ripley"))
				.andExpect(jsonPath("$.cpf").value(cpf))
				.andExpect(jsonPath("$.rg").value("SP-11.222.333"))
				.andExpect(jsonPath("$.birthDate").value("1979-06-22"))
				.andExpect(jsonPath("$.gender").value("F"))
				.andExpect(jsonPath("$.maritalStatus").value("DIV"))
				.andExpect(jsonPath("$.bloodType").value("O-"))
				.andExpect(jsonPath("$.phone").value("11955554444"))
				.andExpect(jsonPath("$.weight").value(62.5))
				.andExpect(jsonPath("$.address.id").value(stored.getAddress().getId()))
				.andExpect(jsonPath("$.address.street").value("Avenida Nostromo"))
				.andExpect(jsonPath("$.address.number").value("S/N"))
				.andExpect(jsonPath("$.address.complement").value("Bloco B"))
				.andExpect(jsonPath("$.address.neighborhood").value("Bairro Alto"))
				.andExpect(jsonPath("$.address.zipCode").value("30140071"))
				.andExpect(jsonPath("$.address.city").value("Belo Horizonte"))
				.andExpect(jsonPath("$.address.state").value("MG"));
		String responseBody = result.andReturn().getResponse().getContentAsString();
		assertThat(Instant.parse(JsonPath.read(responseBody, "$.createdAt"))).isEqualTo(stored.getCreatedAt());
		assertThat(Instant.parse(JsonPath.read(responseBody, "$.updatedAt"))).isEqualTo(stored.getUpdatedAt());
		assertThat(stored.getUpdatedAt()).isEqualTo(Instant.parse("2025-01-02T03:04:05Z"));
		assertThat(repository.count()).isEqualTo(countBefore);
	}

	@Test
	@DisplayName("Dado um id que não existe, quando faço GET /donors/{id}, então devolve 404 no formato padrão e nada muda")
	void returnsNotFoundForUnknownId() throws Exception {
		// Arrange
		long unknownId = 999_999_999L;
		long countBefore = repository.count();
		Instant before = Instant.now();

		// Act
		ResultActions result = mockMvc.perform(get("/donors/{id}", unknownId));
		Instant after = Instant.now();

		// Assert
		assertStandardError(result, 404, "Recurso não encontrado", "Doador não encontrado.",
				"/donors/" + unknownId, before, after);
		assertThat(repository.count()).isEqualTo(countBefore);
	}
}
