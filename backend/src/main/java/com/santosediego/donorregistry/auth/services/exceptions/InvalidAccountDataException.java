package com.santosediego.donorregistry.auth.services.exceptions;

import java.util.List;

/** The username or the password breaks a rule. Carries the rule messages, never the typed values. */
public class InvalidAccountDataException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	private final transient List<String> errors;

	public InvalidAccountDataException(List<String> errors) {
		super("Dados da conta inválidos. " + String.join(" ", errors));
		this.errors = List.copyOf(errors);
	}

	public List<String> getErrors() {
		return errors;
	}
}
