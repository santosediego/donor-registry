package com.santosediego.donorregistry.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AddressRequestDTO(

		@NotBlank(message = "A rua é obrigatória.")
		@Size(max = 150, message = "A rua deve ter no máximo {max} caracteres.")
		String street,

		@NotBlank(message = "O número é obrigatório.")
		@Size(max = 10, message = "O número deve ter no máximo {max} caracteres.")
		String number,

		@Size(max = 100, message = "O complemento deve ter no máximo {max} caracteres.")
		String complement,

		@NotBlank(message = "O bairro é obrigatório.")
		@Size(max = 80, message = "O bairro deve ter no máximo {max} caracteres.")
		String neighborhood,

		@NotNull(message = "O CEP é obrigatório.")
		@Pattern(regexp = "\\d{8}", message = "O CEP deve conter 8 números.")
		String zipCode,

		@NotBlank(message = "A cidade é obrigatória.")
		@Size(max = 100, message = "A cidade deve ter no máximo {max} caracteres.")
		String city,

		@NotNull(message = "O estado é obrigatório.")
		@Pattern(regexp = "[A-Z]{2}", message = "O estado deve conter 2 letras maiúsculas.")
		String state) {
}
