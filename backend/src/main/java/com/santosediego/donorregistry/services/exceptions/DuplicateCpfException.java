package com.santosediego.donorregistry.services.exceptions;

// Never carries data of the existing donor (privacy)
public class DuplicateCpfException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public DuplicateCpfException() {
		super("Já existe um doador cadastrado com este CPF.");
	}
}
