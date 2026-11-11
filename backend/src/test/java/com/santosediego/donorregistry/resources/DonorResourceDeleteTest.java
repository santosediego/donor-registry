package com.santosediego.donorregistry.resources;

import static com.santosediego.donorregistry.support.ErrorAssertions.assertStandardError;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.santosediego.donorregistry.entities.Donor;
import com.santosediego.donorregistry.repositories.DonorRepository;
import com.santosediego.donorregistry.support.ApiTestSupport;
import com.santosediego.donorregistry.support.DonorDatabase;
import com.santosediego.donorregistry.support.TestCpf;

import tools.jackson.databind.json.JsonMapper;

class DonorResourceDeleteTest extends ApiTestSupport {

	private final DonorDatabase db;

	@Autowired
	DonorResourceDeleteTest(MockMvc mockMvc, DonorRepository repository, JsonMapper jsonMapper, JdbcTemplate jdbc) {
		super(mockMvc, repository, jsonMapper);
		this.db = new DonorDatabase(repository, jdbc);
	}

	@Test
	@DisplayName("Dado um doador com endereço, quando faço DELETE /donors/{id}, então devolve 204 sem corpo, o doador e o endereço dele somem e o endereço de outro doador continua")
	void deletesDonorAndItsAddress() throws Exception {
		// Arrange
		Donor target = db.saveDonorWithAddress(TestCpf.next());
		Donor other = db.saveDonorWithAddress(TestCpf.next());
		Long targetAddressId = repository.findById(target.getId()).orElseThrow().getAddress().getId();

		// Act
		ResultActions result = mockMvc.perform(delete("/donors/" + target.getId()));

		// Assert
		result.andExpect(status().isNoContent());
		assertThat(result.andReturn().getResponse().getContentAsString()).isEmpty();
		assertThat(repository.existsById(target.getId())).isFalse();
		assertThat(db.addressRows(target.getId())).isZero();
		assertThat(db.addressRowsWithId(targetAddressId)).isZero();
		assertThat(repository.existsById(other.getId())).isTrue();
		assertThat(db.addressRows(other.getId())).isEqualTo(1);
	}

	@Test
	@DisplayName("Dado o mesmo id já excluído, quando faço o DELETE de novo, então devolve 404 no formato padrão de erro e nada muda")
	void returnsNotFoundWhenDeletingTwice() throws Exception {
		// Arrange
		Donor donor = db.saveDonorWithAddress(TestCpf.next());
		repository.deleteById(donor.getId());
		long countBefore = repository.count();
		Instant before = Instant.now();

		// Act
		ResultActions result = mockMvc.perform(delete("/donors/" + donor.getId()));
		Instant after = Instant.now();

		// Assert
		assertStandardError(result, 404, "Recurso não encontrado", "Doador não encontrado.",
				"/donors/" + donor.getId(), before, after);
		assertThat(repository.count()).isEqualTo(countBefore);
	}

	@Test
	@DisplayName("Dado um id não numérico (abc), quando faço DELETE, então devolve 400 com o parâmetro 'id' inválido e nada muda")
	void rejectsNonNumericId() throws Exception {
		// Arrange
		long countBefore = repository.count();
		Instant before = Instant.now();

		// Act
		ResultActions result = mockMvc.perform(delete("/donors/abc"));
		Instant after = Instant.now();

		// Assert
		assertStandardError(result, 400, "Requisição inválida", "Valor inválido para o parâmetro 'id'.",
				"/donors/abc", before, after);
		assertThat(repository.count()).isEqualTo(countBefore);
	}
}
