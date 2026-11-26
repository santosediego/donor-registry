package com.santosediego.donorregistry.auth.services;

import java.text.Normalizer;

import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Hashes and verifies passwords with Argon2id. The only place where a password is normalized:
 * the same characters can arrive as different code points ("é" as one, or "e" plus an accent),
 * so every password becomes NFC before it is hashed, verified or measured.
 */
public class PasswordHasher {

	/** Longest password accepted, in code points, after normalization. */
	public static final int MAX_LENGTH = 128;

	// Argon2id work factor: OWASP minimum configuration (19 MiB, 2 iterations, parallelism 1)
	private static final int SALT_BYTES = 16;
	private static final int HASH_BYTES = 32;
	private static final int PARALLELISM = 1;
	private static final int MEMORY_KIB = 19_456;
	private static final int ITERATIONS = 2;

	private final PasswordEncoder encoder = new Argon2PasswordEncoder(SALT_BYTES, HASH_BYTES, PARALLELISM,
			MEMORY_KIB, ITERATIONS);

	public static String normalize(String rawPassword) {
		return Normalizer.normalize(rawPassword, Normalizer.Form.NFC);
	}

	/** Returns a new salted hash. The error message never carries the password. */
	public String hash(String rawPassword) {
		String password = prepare(rawPassword);
		if (password == null) {
			throw new IllegalArgumentException(
					"The password to hash must have between 1 and " + MAX_LENGTH + " characters.");
		}
		return encoder.encode(password);
	}

	public boolean matches(String rawPassword, String passwordHash) {
		String password = prepare(rawPassword);
		return password != null && passwordHash != null && encoder.matches(password, passwordHash);
	}

	// Normalized password, or null when it must not reach the algorithm (missing, empty or too long)
	private String prepare(String rawPassword) {
		if (rawPassword == null || rawPassword.isEmpty()) {
			return null;
		}
		String password = normalize(rawPassword);
		return password.codePointCount(0, password.length()) <= MAX_LENGTH ? password : null;
	}
}
