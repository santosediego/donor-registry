package com.santosediego.donorregistry.auth.services.validation;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.santosediego.donorregistry.auth.services.PasswordHasher;

/**
 * Rules for a new password. Lengths are counted in code points (an emoji counts as one), on the
 * normalized form that will be hashed. No composition rules, and nothing is silently trimmed.
 * Messages are shown to the user and never repeat what was typed.
 */
public final class PasswordPolicy {

	public static final int MIN_LENGTH = 15;
	public static final int MAX_LENGTH = PasswordHasher.MAX_LENGTH;

	private PasswordPolicy() {
	}

	/** Returns one message per broken rule; an empty list means the password is accepted. */
	public static List<String> validate(String rawPassword, String username) {
		if (rawPassword == null || rawPassword.isEmpty()) {
			return List.of("A senha é obrigatória.");
		}
		String password = PasswordHasher.normalize(rawPassword);
		int length = password.codePointCount(0, password.length());
		List<String> errors = new ArrayList<>();
		if (length < MIN_LENGTH) {
			errors.add("A senha deve ter pelo menos " + MIN_LENGTH + " caracteres. Use uma frase: várias "
					+ "palavras separadas por espaço são fáceis de lembrar.");
		}
		if (length > MAX_LENGTH) {
			errors.add("A senha deve ter no máximo " + MAX_LENGTH + " caracteres.");
		}
		if (isSpace(password.codePointAt(0)) || isSpace(password.codePointBefore(password.length()))) {
			errors.add("A senha não pode começar nem terminar com espaço.");
		}
		if (password.codePoints().anyMatch(Character::isISOControl)) {
			errors.add("A senha não pode conter caracteres de controle, como tabulação ou quebra de linha.");
		}
		if (password.codePoints().distinct().count() == 1) {
			errors.add("A senha não pode ser formada por um único caractere repetido.");
		}
		if (username != null && password.toLowerCase(Locale.ROOT).equals(UsernamePolicy.normalize(username))) {
			errors.add("A senha não pode ser igual ao nome de usuário.");
		}
		return errors;
	}

	// Includes non-breaking spaces, which Character.isWhitespace leaves out
	private static boolean isSpace(int codePoint) {
		return Character.isWhitespace(codePoint) || Character.isSpaceChar(codePoint);
	}
}
