package com.santosediego.donorregistry.auth.services;

import java.util.ArrayList;
import java.util.List;

import com.santosediego.donorregistry.auth.entities.UserAccount;
import com.santosediego.donorregistry.auth.services.exceptions.InvalidAccountDataException;
import com.santosediego.donorregistry.auth.services.validation.PasswordPolicy;
import com.santosediego.donorregistry.auth.services.validation.UsernamePolicy;

/**
 * The way to build an account from what the user typed: validates both values, normalizes the
 * username and keeps only the password hash. Does not save.
 */
public class UserAccountFactory {

	private final PasswordHasher passwordHasher;

	public UserAccountFactory(PasswordHasher passwordHasher) {
		this.passwordHasher = passwordHasher;
	}

	public UserAccount newAccount(String rawUsername, String rawPassword) {
		List<String> errors = new ArrayList<>(UsernamePolicy.validate(rawUsername));
		errors.addAll(PasswordPolicy.validate(rawPassword, rawUsername));
		if (!errors.isEmpty()) {
			throw new InvalidAccountDataException(errors);
		}
		UserAccount account = new UserAccount();
		account.setUsername(UsernamePolicy.normalize(rawUsername));
		account.setPasswordHash(passwordHasher.hash(rawPassword));
		return account;
	}
}
