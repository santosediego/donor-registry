package com.santosediego.donorregistry.services.exceptions;

import java.util.List;

public class InvalidEnumValueException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public InvalidEnumValueException(String fieldLabel, String value, List<String> acceptedCodes) {
		super("Valor inválido para " + fieldLabel + ": '" + value + "'. Valores aceitos: "
				+ String.join(", ", acceptedCodes) + ".");
	}
}
