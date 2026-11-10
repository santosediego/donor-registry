package com.santosediego.donorregistry.resources;

import static com.santosediego.donorregistry.support.DonorRequests.fullBody;
import static com.santosediego.donorregistry.support.DonorRequests.minimalBody;
import static com.santosediego.donorregistry.support.ErrorAssertions.assertStandardError;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

import java.time.Instant;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.santosediego.donorregistry.dto.DonorDTO;
import com.santosediego.donorregistry.entities.Donor;
import com.santosediego.donorregistry.repositories.DonorRepository;
import com.santosediego.donorregistry.support.ApiTestSupport;
import com.santosediego.donorregistry.support.DonorDatabase;
import com.santosediego.donorregistry.support.TestCpf;

import tools.jackson.databind.json.JsonMapper;

/**
 * Tests the safety net of the unique constraint on tb_donor.cpf: when the prior check
 * (existsByCpf / existsByCpfAndIdNot) does not see the duplicate, as in a race between two requests,
 * the database rejects it and the service must still answer 409.
 *
 * Only place with Mockito: a spy on the repository makes the prior check "blind" (the real method
 * is used everywhere else). The annotation changes the Spring context, so this class lives apart:
 * it starts one extra context and does not affect the other test classes.
 */
@MockitoSpyBean(types = DonorRepository.class)
class DonorResourceCpfRaceTest extends ApiTestSupport {

	private final DonorDatabase db;

	@Autowired
	DonorResourceCpfRaceTest(MockMvc mockMvc, DonorRepository repository, JsonMapper jsonMapper, JdbcTemplate jdbc) {
		super(mockMvc, repository, jsonMapper);
		this.db = new DonorDatabase(repository, jdbc);
	}

	@Test
	@DisplayName("Dado que a checagem prévia de CPF não enxerga o duplicado, quando faço PUT com o CPF de outro doador, então devolve 409 e nenhum dos dois muda")
	void putReturnsConflictWhenPriorCheckMissesDuplicate() throws Exception {
		// Arrange
		Donor first = repository.findById(db.saveDonorWithoutAddress(TestCpf.next()).getId()).orElseThrow();
		Donor secondUnsaved = db.fullDonor(TestCpf.next());
		secondUnsaved.setName("Walter Skinner");
		Donor second = repository.findById(repository.save(secondUnsaved).getId()).orElseThrow();
		DonorDTO firstBefore = new DonorDTO(first);
		DonorDTO secondBefore = new DonorDTO(second);
		doReturn(false).when(repository).existsByCpfAndIdNot(second.getCpf(), first.getId());
		Map<String, Object> body = fullBody();
		body.put("cpf", second.getCpf());
		Instant before = Instant.now();

		// Act
		ResultActions result = mockMvc.perform(put("/donors/" + first.getId())
				.contentType(MediaType.APPLICATION_JSON).content(json(body)));
		Instant after = Instant.now();

		// Assert
		assertStandardError(result, 409, "Conflito", "Já existe um doador cadastrado com este CPF.",
				"/donors/" + first.getId(), before, after);
		String responseBody = result.andReturn().getResponse().getContentAsString();
		assertThat(responseBody).doesNotContain("Walter Skinner").doesNotContain(second.getCpf());
		assertThat(new DonorDTO(repository.findById(first.getId()).orElseThrow())).isEqualTo(firstBefore);
		assertThat(new DonorDTO(repository.findById(second.getId()).orElseThrow())).isEqualTo(secondBefore);
	}

	@Test
	@DisplayName("Dado que a checagem prévia de CPF não enxerga o duplicado, quando faço POST com o CPF de um doador existente, então devolve 409 e continua só um doador com o CPF")
	void postReturnsConflictWhenPriorCheckMissesDuplicate() throws Exception {
		// Arrange
		String cpf = TestCpf.next();
		Donor existing = db.fullDonor(cpf);
		existing.setName("Existing Donor Name");
		repository.save(existing);
		doReturn(false).when(repository).existsByCpf(cpf);
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
				.doesNotContain("21999998888");
		assertThat(repository.count()).isEqualTo(countBefore);
		assertThat(repository.findAll().stream().filter(d -> cpf.equals(d.getCpf()))).hasSize(1);
	}
}
