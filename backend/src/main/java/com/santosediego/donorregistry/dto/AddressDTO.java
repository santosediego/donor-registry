package com.santosediego.donorregistry.dto;

import com.santosediego.donorregistry.entities.Address;

public record AddressDTO(
		Long id,
		String street,
		String number,
		String complement,
		String neighborhood,
		String zipCode,
		String city,
		String state) {

	public AddressDTO(Address entity) {
		this(entity.getId(), entity.getStreet(), entity.getNumber(), entity.getComplement(),
				entity.getNeighborhood(), entity.getZipCode(), entity.getCity(), entity.getState());
	}
}
