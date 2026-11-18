package com.santosediego.donorregistry.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.test.web.servlet.ResultActions;

import com.jayway.jsonpath.JsonPath;

/**
 * Common check for the error format (the five fields). It only does the Assert part:
 * the request and the expected values stay in the test.
 */
public final class ErrorAssertions {

	private ErrorAssertions() {
	}

	public static void assertStandardError(ResultActions result, int status, String error, String message,
			String path, Instant before, Instant after) throws Exception {
		result.andExpect(status().is(status))
				.andExpect(jsonPath("$.status").value(status))
				.andExpect(jsonPath("$.error").value(error))
				.andExpect(jsonPath("$.message").value(message))
				.andExpect(jsonPath("$.path").value(path));
		String timestamp = JsonPath.read(result.andReturn().getResponse().getContentAsString(), "$.timestamp");
		assertThat(Instant.parse(timestamp)).isBetween(before, after);
	}

	/** Checks the `errors` list: exactly these fields with these messages, in any order. */
	public static void assertFieldErrors(ResultActions result, Map<String, String> expected) throws Exception {
		List<Map<String, String>> errors = JsonPath.read(result.andReturn().getResponse().getContentAsString(),
				"$.errors");
		Map<String, String> actual = new HashMap<>();
		errors.forEach(e -> actual.put(e.get("fieldName"), e.get("message")));
		assertThat(errors).as("one entry per field").hasSize(expected.size());
		assertThat(actual).containsExactlyInAnyOrderEntriesOf(expected);
	}

	/**
	 * Like assertFieldErrors, but a field may have more than one message (the Map version
	 * would keep only one per field). Compares (fieldName, message) pairs, in any order.
	 */
	public static void assertFieldErrorList(ResultActions result, List<Map.Entry<String, String>> expected)
			throws Exception {
		List<Map<String, String>> errors = JsonPath.read(result.andReturn().getResponse().getContentAsString(),
				"$.errors");
		List<Map.Entry<String, String>> actual = errors.stream()
				.map(e -> Map.entry(e.get("fieldName"), e.get("message"))).toList();
		assertThat(actual).containsExactlyInAnyOrderElementsOf(expected);
	}
}
