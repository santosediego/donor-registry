package com.santosediego.donorregistry.entities.enums;

public enum MaritalStatus {

	SINGLE("SOL", "Solteiro"),
	MARRIED("CAS", "Casado"),
	SEPARATED("SEP", "Separado"),
	DIVORCED("DIV", "Divorciado"),
	WIDOWED("VIU", "Viúvo"),
	STABLE_UNION("UNI", "União estável");

	private final String id;
	private final String description;

	MaritalStatus(String id, String description) {
		this.id = id;
		this.description = description;
	}

	public String getId() {
		return id;
	}

	public String getDescription() {
		return description;
	}

	public static MaritalStatus fromId(String id) {
		if (id == null) {
			return null;
		}
		for (MaritalStatus value : values()) {
			if (value.id.equals(id)) {
				return value;
			}
		}
		throw new IllegalArgumentException("Invalid marital status: " + id);
	}
}
