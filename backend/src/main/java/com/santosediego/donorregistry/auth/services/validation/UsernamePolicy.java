package com.santosediego.donorregistry.auth.services.validation;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Rules for the username. It is stored normalized, so "Admin" and "admin" are the same user.
 * Messages are shown to the user and never repeat what was typed.
 */
public final class UsernamePolicy {

	public static final int MIN_LENGTH = 3;
	public static final int MAX_LENGTH = 30;

	private static final Pattern ALLOWED = Pattern.compile("[a-z0-9._-]*");

	private UsernamePolicy() {
	}

	/** Without surrounding spaces and in lowercase; null stays null. */
	public static String normalize(String rawUsername) {
		// Locale.ROOT: with a Turkish default locale, "ADMIN" would become "admın"
		return rawUsername == null ? null : rawUsername.strip().toLowerCase(Locale.ROOT);
	}

	/** Validates the normalized form. An empty list means the username is accepted. */
	public static List<String> validate(String rawUsername) {
		String username = normalize(rawUsername);
		if (username == null || username.isEmpty()) {
			return List.of("O nome de usuário é obrigatório.");
		}
		List<String> errors = new ArrayList<>();
		if (username.length() < MIN_LENGTH || username.length() > MAX_LENGTH) {
			errors.add("O nome de usuário deve ter entre " + MIN_LENGTH + " e " + MAX_LENGTH + " caracteres.");
		}
		if (!ALLOWED.matcher(username).matches()) {
			errors.add("O nome de usuário aceita apenas letras sem acento, números, ponto, hífen e sublinhado.");
		}
		return errors;
	}
}
