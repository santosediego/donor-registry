package com.santosediego.donorregistry.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import com.santosediego.donorregistry.entities.Donor;

public record DonorDTO(
		Long id,
		String name,
		String cpf,
		String rg,
		LocalDate birthDate,
		String gender,
		String maritalStatus,
		String bloodType,
		String phone,
		BigDecimal weight,
		AddressDTO address,
		Instant createdAt,
		Instant updatedAt) {

	public DonorDTO(Donor entity) {
		this(entity.getId(), entity.getName(), entity.getCpf(), entity.getRg(), entity.getBirthDate(),
				entity.getGender(), entity.getMaritalStatus(), entity.getBloodType(), entity.getPhone(),
				entity.getWeight(), entity.getAddress() == null ? null : new AddressDTO(entity.getAddress()),
				entity.getCreatedAt(), entity.getUpdatedAt());
	}
}
