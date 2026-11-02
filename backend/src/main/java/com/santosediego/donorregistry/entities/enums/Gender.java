package com.santosediego.donorregistry.entities.enums;

public enum Gender {

	MALE("M", "Masculino"),
	FEMALE("F", "Feminino"),
	OTHER("OT", "Outro");

	private final String id;
	private final String description;

	Gender(String id, String description) {
		this.id = id;
		this.description = description;
	}

	public String getId() {
		return id;
	}

	public String getDescription() {
		return description;
	}

	public static Gender fromId(String id) {
		if (id == null) {
			return null;
		}
		for (Gender value : values()) {
			if (value.id.equals(id)) {
				return value;
			}
		}
		throw new IllegalArgumentException("Invalid gender: " + id);
	}
}
