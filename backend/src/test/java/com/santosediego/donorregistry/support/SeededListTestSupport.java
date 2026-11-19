package com.santosediego.donorregistry.support;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.santosediego.donorregistry.entities.Donor;
import com.santosediego.donorregistry.repositories.DonorRepository;

import tools.jackson.databind.json.JsonMapper;

/**
 * Base for the listing tests (GET /donors). These tests READ the 16 seed donors
 * (changeSets 003/004) and never create or change anything.
 *
 * Two protections keep the seeds intact:
 * 1. A dedicated in-memory H2 database (different name in the URL, same options as the test
 *    profile): Liquibase loads the seeds into it, and no other test class has access to it.
 * 2. A guard before each test: if the database does not hold exactly the 16 expected seeds,
 *    the test fails with one clear message, instead of many confusing count failures.
 */
@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:donor_registry_list_test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1")
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class SeededListTestSupport extends ApiTestSupport {

	/** CPFs of the 16 seed donors (changeSet 003). */
	private static final Set<String> SEED_CPFS = Set.of(
			"00000007447", "00000170275", "00000000272", "00000004006",
			"00000198447", "00000202908", "00000198790", "00000050024",
			"00000050105", "00000170194", "00000001325", "00000009903",
			"00000004774", "00000090085", "00000070050", "00000000515");

	protected SeededListTestSupport(MockMvc mockMvc, DonorRepository repository, JsonMapper jsonMapper) {
		super(mockMvc, repository, jsonMapper);
	}

	@BeforeEach
	void seedsAreExactlyTheExpectedOnes() {
		Set<String> actual = new HashSet<>(repository.findAll().stream().map(Donor::getCpf).toList());
		Set<String> extra = new HashSet<>(actual);
		extra.removeAll(SEED_CPFS);
		Set<String> missing = new HashSet<>(SEED_CPFS);
		missing.removeAll(actual);
		assertThat(actual).as("Expected exactly the 16 seed donors (changeSets 003/004). Extra CPFs: %s; missing CPFs: %s."
				+ " Some test leaked a record, or the seed changed.", extra, missing).isEqualTo(SEED_CPFS);
	}

	/** Id of a seed donor (the database generates it, so tests never hard-code ids). */
	protected Long idOfCpf(String cpf) {
		return repository.findAll().stream().filter(d -> cpf.equals(d.getCpf())).findFirst().orElseThrow().getId();
	}
}
