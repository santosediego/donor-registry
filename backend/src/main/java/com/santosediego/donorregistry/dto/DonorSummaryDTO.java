package com.santosediego.donorregistry.dto;

/**
 * Row of the donor list. Built directly by the JPQL constructor expression in
 * DonorRepository, so the argument order and types here must match that query.
 * {@code bloodType} is already the short code (the entity stores the code as
 * text) and may be null; {@code city} is null when the donor has no address.
 */
public record DonorSummaryDTO(
		Long id,
		String name,
		String phone,
		String cpf,
		String rg,
		String bloodType,
		String city) {
}
