package com.santosediego.donorregistry.support;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Builds request bodies as maps. The test overrides only the field of its scenario
 * (body.put("cpf", ...)), so the scenario stays visible inside the test.
 * All data is fictitious.
 */
public final class DonorRequests {

	private DonorRequests() {
	}

	/** Only the required fields; a new valid CPF on every call. */
	public static Map<String, Object> minimalBody() {
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("name", "Dana Scully");
		body.put("cpf", TestCpf.next());
		body.put("birthDate", "1990-05-20");
		body.put("gender", "F");
		body.put("phone", "11912345678");
		return body;
	}

	/** Every field filled, including the address. */
	public static Map<String, Object> fullBody() {
		Map<String, Object> body = minimalBody();
		body.put("rg", "MG-12.345.678");
		body.put("maritalStatus", "SOL");
		body.put("bloodType", "A+");
		body.put("weight", 70.5);
		body.put("address", validAddress());
		return body;
	}

	public static Map<String, Object> validAddress() {
		Map<String, Object> address = new LinkedHashMap<>();
		address.put("street", "Rua das Flores");
		address.put("number", "123");
		address.put("complement", "Apto 4");
		address.put("neighborhood", "Centro");
		address.put("zipCode", "01310100");
		address.put("city", "São Paulo");
		address.put("state", "SP");
		return address;
	}
}
