package com.santosediego.donorregistry.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.hibernate.validator.constraints.br.CPF;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

// gender, maritalStatus and bloodType arrive as the short code ("M", "SOL", "A+")
// and are converted in the service, so the enums need no Jackson annotations.
public record DonorRequestDTO(

		@NotNull(message = "O nome é obrigatório.")
		@Size(min = 2, max = 120, message = "O nome deve ter entre {min} e {max} caracteres.")
		@Pattern(regexp = "\\S(.*\\S)?", message = "O nome não pode começar nem terminar com espaço.")
		String name,

		@NotNull(message = "O CPF é obrigatório.")
		@Pattern(regexp = "\\d{11}", message = "O CPF deve conter 11 números.")
		@CPF(message = "CPF inválido.")
		String cpf,

		@Size(max = 20, message = "O RG deve ter no máximo {max} caracteres.")
		String rg,

		@NotNull(message = "A data de nascimento é obrigatória.")
		@Past(message = "A data de nascimento deve estar no passado.")
		LocalDate birthDate,

		@NotBlank(message = "O gênero é obrigatório.")
		String gender,

		String maritalStatus,

		String bloodType,

		@NotNull(message = "O telefone é obrigatório.")
		@Pattern(regexp = "\\d{10,11}", message = "O telefone deve conter 10 ou 11 números.")
		String phone,

		@DecimalMin(value = "0", inclusive = false, message = "O peso deve ser maior que zero.")
		@DecimalMax(value = "300", message = "O peso deve ser de no máximo {value} kg.")
		@Digits(integer = 3, fraction = 1, message = "O peso deve ter no máximo uma casa decimal.")
		BigDecimal weight,

		@Valid
		AddressRequestDTO address) {
}
