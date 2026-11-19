package com.santosediego.donorregistry.support;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.test.web.servlet.ResultActions;

import com.jayway.jsonpath.JsonPath;

/**
 * Common checks for the list response. Only the Assert part: the request and the expected
 * values stay in the test.
 */
public final class ListAssertions {

	/** The only fields of an item of `content`. */
	public static final List<String> ITEM_FIELDS = List.of("id", "name", "phone", "cpf", "rg", "bloodType", "city");

	private ListAssertions() {
	}

	private static String body(ResultActions result) throws Exception {
		return result.andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
	}

	/** Names of the items of `content`, in the order of the response. */
	public static List<String> names(ResultActions result) throws Exception {
		return JsonPath.read(body(result), "$.content[*].name");
	}

	/** Checks the item at `index`, field by field. A null expected value means the field is present and null. */
	public static void assertItem(ResultActions result, int index, Long id, String name, String phone, String cpf,
			String rg, String bloodType, String city) throws Exception {
		Map<String, Object> actual = new LinkedHashMap<>(JsonPath.<Map<String, Object>>read(body(result), "$.content[" + index + "]"));
		actual.put("id", ((Number) actual.get("id")).longValue()); // JsonPath returns Integer for small numbers
		Map<String, Object> expected = new LinkedHashMap<>();
		expected.put("id", id);
		expected.put("name", name);
		expected.put("phone", phone);
		expected.put("cpf", cpf);
		expected.put("rg", rg);
		expected.put("bloodType", bloodType);
		expected.put("city", city);
		assertThat(actual).as("item %d of content", index).containsOnlyKeys(ITEM_FIELDS).isEqualTo(expected);
	}

	/** Every item of `content` has exactly the seven fields (a missing field fails; a null value is fine). */
	public static void assertEveryItemHasExactlyTheItemFields(ResultActions result) throws Exception {
		List<Map<String, Object>> items = JsonPath.read(body(result), "$.content");
		assertThat(items).isNotEmpty();
		items.forEach(item -> assertThat(item).containsOnlyKeys(ITEM_FIELDS));
	}

	/** Checks the four fields of `page`. */
	public static void assertPage(ResultActions result, int size, int number, int totalElements, int totalPages)
			throws Exception {
		Map<String, Object> page = JsonPath.read(body(result), "$.page");
		assertThat(page).containsOnlyKeys("size", "number", "totalElements", "totalPages");
		assertThat(((Number) page.get("size")).intValue()).as("page.size").isEqualTo(size);
		assertThat(((Number) page.get("number")).intValue()).as("page.number").isEqualTo(number);
		assertThat(((Number) page.get("totalElements")).intValue()).as("page.totalElements").isEqualTo(totalElements);
		assertThat(((Number) page.get("totalPages")).intValue()).as("page.totalPages").isEqualTo(totalPages);
	}
}
