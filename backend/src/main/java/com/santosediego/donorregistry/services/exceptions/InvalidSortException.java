package com.santosediego.donorregistry.services.exceptions;

import java.util.List;

public class InvalidSortException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public InvalidSortException(List<String> acceptedFields) {
		super("Campo de ordenação inválido. Campos aceitos: " + String.join(", ", acceptedFields)
				+ ". Use, por exemplo, sort=name,asc.");
	}
}
