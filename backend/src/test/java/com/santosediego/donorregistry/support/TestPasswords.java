package com.santosediego.donorregistry.support;

import java.util.UUID;

/** Generates fictitious passwords that follow the rules. A new one on every call; none is written in the code. */
public final class TestPasswords {

	private TestPasswords() {
	}

	public static String next() {
		return "frase de teste " + UUID.randomUUID();
	}
}
