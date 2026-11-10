package com.santosediego.donorregistry.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;

import org.springframework.test.web.servlet.ResultActions;

import com.jayway.jsonpath.JsonPath;
import com.santosediego.donorregistry.entities.Address;
import com.santosediego.donorregistry.entities.Donor;

/**
 * Compares a donor (response body or database row) with the request body map.
 * Does not check id, createdAt or updatedAt: each test does that with its own rule
 * (same value, or inside the request window).
 */
public final class DonorResponseAssertions {

	private static final String[] TEXT_FIELDS = { "name", "cpf", "rg", "birthDate", "gender", "maritalStatus",
			"bloodType", "phone" };
	private static final String[] ADDRESS_FIELDS = { "street", "number", "complement", "neighborhood", "zipCode",
			"city", "state" };

	private DonorResponseAssertions() {
	}

	/** Fields of the response body against the expected values (a missing key means null). */
	@SuppressWarnings("unchecked")
	public static void assertResponseFields(ResultActions result, Map<String, Object> expected) throws Exception {
		for (String field : TEXT_FIELDS) {
			expectValue(result, "$." + field, expected.get(field));
		}
		Object weight = expected.get("weight");
		expectValue(result, "$.weight", weight == null ? null : ((Number) weight).doubleValue());
		Map<String, Object> address = (Map<String, Object>) expected.get("address");
		if (address == null) {
			result.andExpect(jsonPath("$.address").value(nullValue()));
			return;
		}
		for (String field : ADDRESS_FIELDS) {
			expectValue(result, "$.address." + field, address.get(field));
		}
	}

	/** Fields of the row read back from the database against the expected values. */
	@SuppressWarnings("unchecked")
	public static void assertStoredFields(Donor stored, Map<String, Object> expected) {
		assertThat(stored.getName()).isEqualTo(expected.get("name"));
		assertThat(stored.getCpf()).isEqualTo(expected.get("cpf"));
		assertThat(stored.getRg()).isEqualTo(expected.get("rg"));
		assertThat(String.valueOf(stored.getBirthDate())).isEqualTo(String.valueOf(expected.get("birthDate")));
		assertThat(stored.getGender()).isEqualTo(expected.get("gender"));
		assertThat(stored.getMaritalStatus()).isEqualTo(expected.get("maritalStatus"));
		assertThat(stored.getBloodType()).isEqualTo(expected.get("bloodType"));
		assertThat(stored.getPhone()).isEqualTo(expected.get("phone"));
		Object weight = expected.get("weight");
		if (weight == null) {
			assertThat(stored.getWeight()).isNull();
		} else {
			assertThat(stored.getWeight()).isEqualByComparingTo(new BigDecimal(weight.toString()));
		}
		Map<String, Object> address = (Map<String, Object>) expected.get("address");
		if (address == null) {
			assertThat(stored.getAddress()).isNull();
			return;
		}
		Address a = stored.getAddress();
		assertThat(a).isNotNull();
		assertThat(a.getStreet()).isEqualTo(address.get("street"));
		assertThat(a.getNumber()).isEqualTo(address.get("number"));
		assertThat(a.getComplement()).isEqualTo(address.get("complement"));
		assertThat(a.getNeighborhood()).isEqualTo(address.get("neighborhood"));
		assertThat(a.getZipCode()).isEqualTo(address.get("zipCode"));
		assertThat(a.getCity()).isEqualTo(address.get("city"));
		assertThat(a.getState()).isEqualTo(address.get("state"));
	}

	public static Instant instantAt(ResultActions result, String path) throws Exception {
		String value = JsonPath.read(result.andReturn().getResponse().getContentAsString(), path);
		return Instant.parse(value);
	}

	public static long longAt(ResultActions result, String path) throws Exception {
		Number value = JsonPath.read(result.andReturn().getResponse().getContentAsString(), path);
		return value.longValue();
	}

	/**
	 * The response carries nanoseconds (generated in memory); the database keeps microseconds.
	 * Known behavior: compare at microsecond precision.
	 */
	public static Instant micros(Instant instant) {
		return instant.truncatedTo(ChronoUnit.MICROS);
	}

	private static void expectValue(ResultActions result, String path, Object value) throws Exception {
		if (value == null) {
			result.andExpect(jsonPath(path).value(nullValue()));
		} else {
			result.andExpect(jsonPath(path).value(value));
		}
	}
}
