package com.santosediego.donorregistry.support;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.santosediego.donorregistry.entities.Donor;
import com.santosediego.donorregistry.repositories.DonorRepository;

import tools.jackson.databind.json.JsonMapper;

/**
 * Base for API tests: real application, H2 (profile test), real commits (no @Transactional).
 * Isolation by explicit cleanup: before each test we remember which donors exist (the seeds);
 * after each test we delete the ones that were not there. So no test leaves data for the next,
 * and the seeds stay untouched.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class ApiTestSupport {

	protected final MockMvc mockMvc;
	protected final DonorRepository repository;
	private final JsonMapper jsonMapper;

	private Set<Long> idsBeforeTest;

	protected ApiTestSupport(MockMvc mockMvc, DonorRepository repository, JsonMapper jsonMapper) {
		this.mockMvc = mockMvc;
		this.repository = repository;
		this.jsonMapper = jsonMapper;
	}

	@BeforeEach
	void rememberExistingDonors() {
		idsBeforeTest = new HashSet<>(repository.findAll().stream().map(Donor::getId).toList());
	}

	@AfterEach
	void deleteDonorsCreatedByTheTest() {
		List<Donor> created = repository.findAll().stream().filter(d -> !idsBeforeTest.contains(d.getId())).toList();
		repository.deleteAll(created); // cascade removes the address
	}

	protected String json(Object body) {
		return jsonMapper.writeValueAsString(body);
	}

	/** Unsaved donor with the required fields only (fictitious data). */
	protected Donor newDonor(String cpf, String name) {
		Donor donor = new Donor();
		donor.setName(name);
		donor.setCpf(cpf);
		donor.setBirthDate(LocalDate.of(1985, 3, 10));
		donor.setGender("M");
		donor.setPhone("21987654321");
		return donor;
	}
}
