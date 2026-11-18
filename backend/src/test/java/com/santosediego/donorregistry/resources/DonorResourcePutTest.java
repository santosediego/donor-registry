package com.santosediego.donorregistry.resources;

import static com.santosediego.donorregistry.support.DonorRequests.bodyOf;
import static com.santosediego.donorregistry.support.DonorRequests.fullBody;
import static com.santosediego.donorregistry.support.DonorRequests.minimalBody;
import static com.santosediego.donorregistry.support.DonorRequests.validAddress;
import static com.santosediego.donorregistry.support.DonorResponseAssertions.assertResponseFields;
import static com.santosediego.donorregistry.support.DonorResponseAssertions.assertStoredFields;
import static com.santosediego.donorregistry.support.DonorResponseAssertions.instantAt;
import static com.santosediego.donorregistry.support.DonorResponseAssertions.longAt;
import static com.santosediego.donorregistry.support.DonorResponseAssertions.micros;
import static com.santosediego.donorregistry.support.ErrorAssertions.assertFieldErrorList;
import static com.santosediego.donorregistry.support.ErrorAssertions.assertFieldErrors;
import static com.santosediego.donorregistry.support.ErrorAssertions.assertStandardError;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.santosediego.donorregistry.dto.DonorDTO;
import com.santosediego.donorregistry.entities.Donor;
import com.santosediego.donorregistry.repositories.DonorRepository;
import com.santosediego.donorregistry.support.ApiTestSupport;
import com.santosediego.donorregistry.support.DonorDatabase;
import com.santosediego.donorregistry.support.TestCpf;

import tools.jackson.databind.json.JsonMapper;

class DonorResourcePutTest extends ApiTestSupport {

	private static final long MISSING_ID = 999_999_999L;

	private final DonorDatabase db;

	@Autowired
	DonorResourcePutTest(MockMvc mockMvc, DonorRepository repository, JsonMapper jsonMapper, JdbcTemplate jdbc) {
		super(mockMvc, repository, jsonMapper);
		this.db = new DonorDatabase(repository, jdbc);
	}

	@Test
	@DisplayName("Dado um doador existente, quando faço PUT com dados novos sem mudar o CPF, então devolve 200 com os dados enviados, ids e createdAt originais e updatedAt na janela")
	void updatesDonorKeepingCpf() throws Exception {
		// Arrange
		Donor original = reload(db.saveDonorWithAddress(TestCpf.next()));
		Map<String, Object> body = fullBody();
		body.put("cpf", original.getCpf());
		Instant before = Instant.now();

		// Act
		ResultActions result = putDonor(original.getId(), body);
		Instant after = Instant.now();

		// Assert
		assertUpdated(result, original, body, before, after);
	}

	@Test
	@DisplayName("Dado um doador existente, quando faço PUT mudando o CPF para um CPF válido e livre, então grava o CPF novo e um POST com o CPF antigo passa a funcionar")
	void updatesCpfAndFreesTheOldOne() throws Exception {
		// Arrange
		String oldCpf = TestCpf.next();
		Donor original = reload(db.saveDonorWithoutAddress(oldCpf));
		Map<String, Object> body = fullBody();
		String newCpf = (String) body.get("cpf");
		Instant before = Instant.now();

		// Act
		ResultActions result = putDonor(original.getId(), body);
		Instant after = Instant.now();

		// Assert: response and database
		assertUpdated(result, original, body, before, after);
		result.andExpect(jsonPath("$.cpf").value(newCpf));
		assertThat(repository.findById(original.getId()).orElseThrow().getCpf()).isEqualTo(newCpf);
		// Assert: the old CPF is free again
		Map<String, Object> postBody = minimalBody();
		postBody.put("cpf", oldCpf);
		mockMvc.perform(post("/donors").contentType(MediaType.APPLICATION_JSON).content(json(postBody)))
				.andExpect(status().isCreated());
	}

	@Test
	@DisplayName("Dado dois doadores, quando faço PUT no primeiro com o CPF do segundo, então devolve 409 sem dados do outro doador e nenhum dos dois muda")
	void rejectsCpfOfAnotherDonor() throws Exception {
		// Arrange
		Donor first = reload(db.saveDonorWithoutAddress(TestCpf.next()));
		Donor secondUnsaved = db.fullDonor(TestCpf.next());
		secondUnsaved.setName("Walter Skinner");
		Donor second = reload(repository.save(secondUnsaved));
		DonorDTO firstBefore = new DonorDTO(first);
		DonorDTO secondBefore = new DonorDTO(second);
		Map<String, Object> body = fullBody();
		body.put("cpf", second.getCpf());
		Instant before = Instant.now();

		// Act
		ResultActions result = putDonor(first.getId(), body);
		Instant after = Instant.now();

		// Assert
		assertStandardError(result, 409, "Conflito", "Já existe um doador cadastrado com este CPF.",
				"/donors/" + first.getId(), before, after);
		String responseBody = result.andReturn().getResponse().getContentAsString();
		assertThat(responseBody).doesNotContain("Walter Skinner").doesNotContain(second.getCpf());
		assertThat(snapshot(first.getId())).isEqualTo(firstBefore);
		assertThat(snapshot(second.getId())).isEqualTo(secondBefore);
		assertThat(snapshot(first.getId()).updatedAt()).isEqualTo(DonorDatabase.OLD_UPDATED_AT);
	}

	@Test
	@DisplayName("Dado um doador sem endereço, quando faço PUT com endereço, então devolve 200 com o endereço novo e o endereço fica gravado e ligado ao doador")
	void createsAddressOnUpdate() throws Exception {
		// Arrange
		Donor original = reload(db.saveDonorWithoutAddress(TestCpf.next()));
		Map<String, Object> body = fullBody();
		body.put("cpf", original.getCpf());
		Instant before = Instant.now();

		// Act
		ResultActions result = putDonor(original.getId(), body);
		Instant after = Instant.now();

		// Assert
		assertUpdated(result, original, body, before, after);
		result.andExpect(jsonPath("$.address.id").value(notNullValue()));
		Donor stored = repository.findById(original.getId()).orElseThrow();
		assertThat(db.addressRows(original.getId())).isEqualTo(1);
		assertThat(stored.getAddress().getDonor().getId()).isEqualTo(original.getId());
	}

	@Test
	@DisplayName("Dado um doador com endereço, quando faço PUT com outro endereço, então o endereço é atualizado no lugar, com o mesmo id, e continua um só endereço")
	void updatesAddressInPlace() throws Exception {
		// Arrange
		Donor original = reload(db.saveDonorWithAddress(TestCpf.next()));
		Long addressId = original.getAddress().getId();
		Map<String, Object> body = fullBody();
		body.put("cpf", original.getCpf());
		Instant before = Instant.now();

		// Act
		ResultActions result = putDonor(original.getId(), body);
		Instant after = Instant.now();

		// Assert
		assertUpdated(result, original, body, before, after);
		result.andExpect(jsonPath("$.address.id").value(addressId));
		assertThat(db.addressRows(original.getId())).isEqualTo(1);
		assertThat(db.addressRowsWithId(addressId)).isEqualTo(1);
		assertThat(repository.findById(original.getId()).orElseThrow().getAddress().getId()).isEqualTo(addressId);
	}

	@Test
	@DisplayName("Dado um doador com endereço, quando faço PUT com address nulo, então devolve 200 com address nulo e o endereço é removido")
	void removesAddressWhenAddressIsNull() throws Exception {
		// Arrange
		Donor original = reload(db.saveDonorWithAddress(TestCpf.next()));
		Long addressId = original.getAddress().getId();
		Map<String, Object> body = fullBody();
		body.put("cpf", original.getCpf());
		body.put("address", null);
		Instant before = Instant.now();

		// Act
		ResultActions result = putDonor(original.getId(), body);
		Instant after = Instant.now();

		// Assert
		assertUpdated(result, original, body, before, after);
		assertThat(db.addressRows(original.getId())).isZero();
		assertThat(db.addressRowsWithId(addressId)).isZero();
	}

	@Test
	@DisplayName("Dado um doador com endereço, quando faço PUT com \"address\": {}, então devolve 400 listando os seis campos obrigatórios do endereço e nada muda")
	void rejectsEmptyAddressObject() throws Exception {
		// Arrange
		Donor original = reload(db.saveDonorWithAddress(TestCpf.next()));
		DonorDTO snapshotBefore = new DonorDTO(original);
		Map<String, Object> body = fullBody();
		body.put("cpf", original.getCpf());
		body.put("address", new LinkedHashMap<>());
		Instant before = Instant.now();

		// Act
		ResultActions result = putDonor(original.getId(), body);
		Instant after = Instant.now();

		// Assert
		assertStandardError(result, 400, "Dados inválidos", "Há campos com valores inválidos.",
				"/donors/" + original.getId(), before, after);
		assertFieldErrors(result, Map.of(
				"address.street", "A rua é obrigatória.",
				"address.number", "O número é obrigatório.",
				"address.neighborhood", "O bairro é obrigatório.",
				"address.zipCode", "O CEP é obrigatório.",
				"address.city", "A cidade é obrigatória.",
				"address.state", "O estado é obrigatório."));
		assertThat(snapshot(original.getId())).isEqualTo(snapshotBefore);
	}

	@Test
	@DisplayName("Dado um doador com bloodType e weight preenchidos, quando faço PUT omitindo os dois, então devolve 200 com os dois nulos e eles ficam nulos no banco")
	void clearsOmittedOptionalFields() throws Exception {
		// Arrange
		Donor original = reload(db.saveDonorWithoutAddress(TestCpf.next()));
		assertThat(original.getBloodType()).isNotNull();
		assertThat(original.getWeight()).isNotNull();
		Map<String, Object> body = fullBody();
		body.put("cpf", original.getCpf());
		body.remove("bloodType");
		body.remove("weight");
		Instant before = Instant.now();

		// Act
		ResultActions result = putDonor(original.getId(), body);
		Instant after = Instant.now();

		// Assert
		assertUpdated(result, original, body, before, after);
		result.andExpect(jsonPath("$.bloodType").value(nullValue()))
				.andExpect(jsonPath("$.weight").value(nullValue()));
		Donor stored = repository.findById(original.getId()).orElseThrow();
		assertThat(stored.getBloodType()).isNull();
		assertThat(stored.getWeight()).isNull();
	}

	@Test
	@DisplayName("Dado um doador, quando faço PUT idêntico ao que está gravado, então devolve 200 com o corpo igual e updatedAt igual ao que estava")
	void keepsUpdatedAtWhenNothingChanged() throws Exception {
		// Arrange
		Donor original = reload(db.saveDonorWithAddress(TestCpf.next()));
		DonorDTO snapshotBefore = new DonorDTO(original);
		Map<String, Object> body = bodyOf(original);

		// Act
		ResultActions result = putDonor(original.getId(), body);

		// Assert
		assertUnchanged(result, original, snapshotBefore, bodyOf(original));
	}

	@Test
	@DisplayName("Dado um doador com peso 70.0 gravado, quando faço PUT idêntico com o peso enviado como 70, então updatedAt continua igual e nada muda")
	void keepsUpdatedAtWhenWeightIsSentWithoutDecimal() throws Exception {
		// Arrange
		Donor original = reload(db.saveDonorWithAddress(TestCpf.next()));
		DonorDTO snapshotBefore = new DonorDTO(original);
		Map<String, Object> body = bodyOf(original);
		body.put("weight", 70);

		// Act
		ResultActions result = putDonor(original.getId(), body);

		// Assert
		assertUnchanged(result, original, snapshotBefore, bodyOf(original));
	}

	@Test
	@DisplayName("Dado um endereço com complement nulo gravado, quando faço PUT idêntico com complement \"\", então updatedAt continua igual e nada muda")
	void keepsUpdatedAtWhenComplementIsSentEmpty() throws Exception {
		// Arrange
		Donor original = reload(db.saveDonorWithAddress(TestCpf.next(), null));
		DonorDTO snapshotBefore = new DonorDTO(original);
		Map<String, Object> body = bodyOf(original);
		@SuppressWarnings("unchecked")
		Map<String, Object> address = (Map<String, Object>) body.get("address");
		address.put("complement", "");

		// Act
		ResultActions result = putDonor(original.getId(), body);

		// Assert
		assertUnchanged(result, original, snapshotBefore, bodyOf(original));
	}

	@Test
	@DisplayName("Dado um doador com endereço, quando faço PUT mudando só o endereço, então devolve 200 com o endereço novo, mesmo id de endereço e updatedAt na janela")
	void updatesUpdatedAtWhenOnlyAddressChanges() throws Exception {
		// Arrange
		Donor original = reload(db.saveDonorWithAddress(TestCpf.next()));
		Long addressId = original.getAddress().getId();
		Map<String, Object> body = bodyOf(original);
		Map<String, Object> newAddress = validAddress();
		body.put("address", newAddress);
		Instant before = Instant.now();

		// Act
		ResultActions result = putDonor(original.getId(), body);
		Instant after = Instant.now();

		// Assert
		assertUpdated(result, original, body, before, after);
		result.andExpect(jsonPath("$.address.id").value(addressId));
	}

	@Test
	@DisplayName("Dado um id que não existe, quando faço PUT, então devolve 404 e nada é criado")
	void returnsNotFoundForMissingId() throws Exception {
		// Arrange
		Map<String, Object> body = fullBody();
		long countBefore = repository.count();
		Instant before = Instant.now();

		// Act
		ResultActions result = putDonor(MISSING_ID, body);
		Instant after = Instant.now();

		// Assert
		assertStandardError(result, 404, "Recurso não encontrado", "Doador não encontrado.",
				"/donors/" + MISSING_ID, before, after);
		assertThat(repository.count()).isEqualTo(countBefore);
	}

	@Test
	@DisplayName("Dado um corpo com JSON quebrado, quando faço PUT, então devolve 400 de corpo inválido e o doador fica como estava")
	void rejectsBrokenJson() throws Exception {
		// Arrange
		Donor original = reload(db.saveDonorWithAddress(TestCpf.next()));
		DonorDTO snapshotBefore = new DonorDTO(original);
		Instant before = Instant.now();

		// Act
		ResultActions result = putDonor(original.getId(), "{ \"name\": \"Dana Scully\", ");
		Instant after = Instant.now();

		// Assert
		assertStandardError(result, 400, "Requisição inválida",
				"Corpo da requisição inválido. Verifique o JSON e as datas enviadas.",
				"/donors/" + original.getId(), before, after);
		assertThat(snapshot(original.getId())).isEqualTo(snapshotBefore);
	}

	@Test
	@DisplayName("Dado name vazio (\"\"), quando faço PUT, então devolve 400 com as duas mensagens do campo name e o doador fica como estava")
	void rejectsEmptyName() throws Exception {
		// Arrange
		Donor original = reload(db.saveDonorWithAddress(TestCpf.next()));
		DonorDTO snapshotBefore = new DonorDTO(original);
		Map<String, Object> body = bodyOf(original);
		body.put("name", "");
		Instant before = Instant.now();

		// Act
		ResultActions result = putDonor(original.getId(), body);
		Instant after = Instant.now();

		// Assert
		assertStandardError(result, 400, "Dados inválidos", "Há campos com valores inválidos.",
				"/donors/" + original.getId(), before, after);
		assertFieldErrorList(result, List.of(
				Map.entry("name", "O nome deve ter entre 2 e 120 caracteres."),
				Map.entry("name", "O nome não pode começar nem terminar com espaço.")));
		assertThat(snapshot(original.getId())).isEqualTo(snapshotBefore);
	}

	@Test
	@DisplayName("Dado um corpo com id, createdAt e updatedAt diferentes dos reais, quando faço PUT, então esses valores são ignorados e id e createdAt continuam os reais")
	void ignoresIdAndTimestampsFromBody() throws Exception {
		// Arrange
		Donor original = reload(db.saveDonorWithAddress(TestCpf.next()));
		Map<String, Object> body = bodyOf(original);
		body.put("name", "Dana Scully"); // a real change, so updatedAt is expected to move
		body.put("id", 999_999L);
		body.put("createdAt", "2000-01-01T00:00:00Z");
		body.put("updatedAt", "2001-01-01T00:00:00Z");
		Instant before = Instant.now();

		// Act
		ResultActions result = putDonor(original.getId(), body);
		Instant after = Instant.now();

		// Assert
		assertUpdated(result, original, body, before, after);
	}

	// ---- helpers ----

	private ResultActions putDonor(long id, Map<String, Object> body) throws Exception {
		return putDonor(id, json(body));
	}

	private ResultActions putDonor(long id, String content) throws Exception {
		return mockMvc.perform(put("/donors/" + id).contentType(MediaType.APPLICATION_JSON).content(content));
	}

	/** Reads the donor back from the database (so timestamps have the precision the database keeps). */
	private Donor reload(Donor saved) {
		return repository.findById(saved.getId()).orElseThrow();
	}

	private DonorDTO snapshot(Long id) {
		return new DonorDTO(repository.findById(id).orElseThrow());
	}

	/**
	 * PUT that changed something: 200, fields equal to the body, ids and createdAt as before,
	 * updatedAt inside the request window, and the database equal to what was sent.
	 */
	private void assertUpdated(ResultActions result, Donor original, Map<String, Object> body, Instant before,
			Instant after) throws Exception {
		result.andExpect(status().isOk());
		assertResponseFields(result, body);
		assertThat(longAt(result, "$.id")).isEqualTo(original.getId());
		if (original.getAddress() != null && body.get("address") != null) {
			assertThat(longAt(result, "$.address.id")).isEqualTo(original.getAddress().getId());
		}
		assertThat(micros(instantAt(result, "$.createdAt"))).isEqualTo(original.getCreatedAt());
		Instant responseUpdatedAt = instantAt(result, "$.updatedAt");
		assertThat(responseUpdatedAt).isBetween(before, after);

		Donor stored = repository.findById(original.getId()).orElseThrow();
		assertStoredFields(stored, body);
		assertThat(stored.getId()).isEqualTo(original.getId());
		assertThat(stored.getCreatedAt()).isEqualTo(original.getCreatedAt());
		assertThat(stored.getUpdatedAt()).isEqualTo(micros(responseUpdatedAt));
	}

	/** PUT that changed nothing: 200, same fields, updatedAt exactly as it was, database untouched. */
	private void assertUnchanged(ResultActions result, Donor original, DonorDTO snapshotBefore,
			Map<String, Object> expected) throws Exception {
		result.andExpect(status().isOk());
		assertResponseFields(result, expected);
		assertThat(instantAt(result, "$.updatedAt")).isEqualTo(DonorDatabase.OLD_UPDATED_AT);
		assertThat(snapshot(original.getId())).isEqualTo(snapshotBefore);
	}
}
