package com.santosediego.donorregistry.repositories;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import com.santosediego.donorregistry.support.ApiTestSupport;
import com.santosediego.donorregistry.support.DonorDatabase;
import com.santosediego.donorregistry.support.TestCpf;

import tools.jackson.databind.json.JsonMapper;

/**
 * NOT NULL on tb_address. Inserts with plain SQL, so it is the database that accepts or rejects
 * (the API validation is not involved). Donor cleanup comes from ApiTestSupport.
 */
class AddressRequiredColumnsTest extends ApiTestSupport {

	private final JdbcTemplate jdbc;
	private final DonorDatabase db;

	@Autowired
	AddressRequiredColumnsTest(MockMvc mockMvc, DonorRepository repository, JsonMapper jsonMapper, JdbcTemplate jdbc) {
		super(mockMvc, repository, jsonMapper);
		this.jdbc = jdbc;
		this.db = new DonorDatabase(repository, jdbc);
	}

	@ParameterizedTest(name = "{0} nulo")
	@ValueSource(strings = { "street", "street_number", "neighborhood", "zip_code", "city", "state" })
	@DisplayName("Dado um endereço com uma coluna obrigatória nula, quando grava no banco, então o banco recusa")
	void databaseRejectsNullInRequiredColumn(String column) {
		// Arrange
		Long donorId = db.saveDonorWithoutAddress(TestCpf.next()).getId();
		Map<String, Object> address = validAddressColumns(donorId);
		address.put(column, null);

		// Act
		Throwable thrown = catchThrowable(() -> insertAddress(address));

		// Assert
		assertThat(thrown).isInstanceOf(DataIntegrityViolationException.class);
		String reason = NestedExceptionUtils.getMostSpecificCause(thrown).getMessage().split("; SQL statement")[0];
		assertThat(reason.toLowerCase(Locale.ROOT)).contains("\"" + column + "\"");
		assertThat(db.addressRows(donorId)).isZero();
	}

	@Test
	@DisplayName("Dado um endereço com complement nulo, quando grava no banco, então é aceito")
	void databaseAcceptsNullComplement() {
		// Arrange
		Long donorId = db.saveDonorWithoutAddress(TestCpf.next()).getId();
		Map<String, Object> address = validAddressColumns(donorId);
		address.put("complement", null);

		// Act
		insertAddress(address);

		// Assert
		assertThat(db.addressRows(donorId)).isEqualTo(1);
	}

	private Map<String, Object> validAddressColumns(Long donorId) {
		Map<String, Object> columns = new LinkedHashMap<>();
		columns.put("street", "Rua Um");
		columns.put("street_number", "10");
		columns.put("complement", "Fundos");
		columns.put("neighborhood", "Jardim");
		columns.put("zip_code", "30140071");
		columns.put("city", "Belo Horizonte");
		columns.put("state", "MG");
		columns.put("donor_id", donorId);
		return columns;
	}

	// Column names come from the test itself, never from outside
	private void insertAddress(Map<String, Object> columns) {
		String names = String.join(", ", columns.keySet());
		String marks = String.join(", ", columns.keySet().stream().map(name -> "?").toList());
		jdbc.update("INSERT INTO tb_address (" + names + ") VALUES (" + marks + ")", columns.values().toArray());
	}
}
