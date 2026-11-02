package com.santosediego.donorregistry.entities.enums;

public enum BloodType {

	A_POSITIVE("A+", "A+"),
	B_POSITIVE("B+", "B+"),
	AB_POSITIVE("AB+", "AB+"),
	O_POSITIVE("O+", "O+"),
	A_NEGATIVE("A-", "A-"),
	B_NEGATIVE("B-", "B-"),
	AB_NEGATIVE("AB-", "AB-"),
	O_NEGATIVE("O-", "O-"),
	FALSE_O_NEGATIVE("FO-", "Falso O-"),
	GOLDEN_BLOOD("SD", "Sangue dourado");

	private final String id;
	private final String description;

	BloodType(String id, String description) {
		this.id = id;
		this.description = description;
	}

	public String getId() {
		return id;
	}

	public String getDescription() {
		return description;
	}

	public static BloodType fromId(String id) {
		if (id == null) {
			return null;
		}
		for (BloodType value : values()) {
			if (value.id.equals(id)) {
				return value;
			}
		}
		throw new IllegalArgumentException("Invalid blood type: " + id);
	}
}
