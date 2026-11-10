package com.santosediego.donorregistry.support;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Generates fictitious CPFs that are valid (correct check digits) and unique within the test run.
 * The base starts at 5xxxxxxxx, so it never collides with the seed data (which starts with 0000).
 */
public final class TestCpf {

	private static final AtomicInteger COUNTER = new AtomicInteger(500_000_000);

	private TestCpf() {
	}

	public static String next() {
		String base = String.valueOf(COUNTER.incrementAndGet());
		int first = checkDigit(base, 10);
		int second = checkDigit(base + first, 11);
		return base + first + second;
	}

	// Standard mod-11 check digit: weights go from startWeight down to 2
	private static int checkDigit(String digits, int startWeight) {
		int sum = 0;
		for (int i = 0; i < digits.length(); i++) {
			sum += (digits.charAt(i) - '0') * (startWeight - i);
		}
		int remainder = sum % 11;
		return remainder < 2 ? 0 : 11 - remainder;
	}
}
