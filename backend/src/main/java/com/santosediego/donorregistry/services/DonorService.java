package com.santosediego.donorregistry.services;

import java.time.Instant;
import java.util.List;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.santosediego.donorregistry.dto.AddressRequestDTO;
import com.santosediego.donorregistry.dto.DonorDTO;
import com.santosediego.donorregistry.dto.DonorRequestDTO;
import com.santosediego.donorregistry.dto.DonorSummaryDTO;
import com.santosediego.donorregistry.entities.Address;
import com.santosediego.donorregistry.entities.Donor;
import com.santosediego.donorregistry.entities.enums.BloodType;
import com.santosediego.donorregistry.entities.enums.Gender;
import com.santosediego.donorregistry.entities.enums.MaritalStatus;
import com.santosediego.donorregistry.repositories.DonorRepository;
import com.santosediego.donorregistry.repositories.DonorSearchQueries;
import com.santosediego.donorregistry.services.exceptions.DuplicateCpfException;
import com.santosediego.donorregistry.services.exceptions.InvalidSortException;
import com.santosediego.donorregistry.services.exceptions.ResourceNotFoundException;

@Service
public class DonorService {

	// Name of the unique constraint on tb_donor.cpf (see changeSet 001)
	private static final String CPF_CONSTRAINT = "uk_tb_donor_cpf";

	// Fields the list can be sorted by (property names of Donor)
	private static final List<String> SORTABLE_FIELDS = List.of("name", "createdAt");

	private final DonorRepository repository;

	public DonorService(DonorRepository repository) {
		this.repository = repository;
	}

	@Transactional(readOnly = true)
	public DonorDTO findById(Long id) {
		Donor entity = repository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Doador não encontrado."));
		return new DonorDTO(entity);
	}

	@Transactional(readOnly = true)
	public Page<DonorSummaryDTO> findAllPaged(String search, Pageable pageable) {
		Pageable safe = withValidSort(pageable);
		String term = search == null ? "" : search.trim();
		if (term.isEmpty()) {
			return repository.search(false, "", false, "", "", safe);
		}
		String cpfDigits = term.replaceAll("[.\\-\\s]", "");
		boolean cpfSearch = cpfDigits.matches("\\d+");
		return repository.search(true, DonorSearchQueries.likePattern(term), cpfSearch, cpfSearch ? DonorSearchQueries.likePattern(cpfDigits) : "",
				DonorSearchQueries.rgPattern(term), safe);
	}

	// Only whitelisted fields; id is added as tie-breaker so pages are stable
	private Pageable withValidSort(Pageable pageable) {
		for (Sort.Order order : pageable.getSort()) {
			if (!SORTABLE_FIELDS.contains(order.getProperty())) {
				throw new InvalidSortException(SORTABLE_FIELDS);
			}
		}
		Sort sort = pageable.getSort().isSorted() ? pageable.getSort() : Sort.by("name");
		return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort.and(Sort.by("id")));
	}

	@Transactional
	public DonorDTO insert(DonorRequestDTO dto) {
		if (repository.existsByCpf(dto.cpf())) {
			throw new DuplicateCpfException();
		}
		Donor entity = new Donor();
		copyDtoToEntity(dto, entity);
		try {
			// saveAndFlush so a unique-constraint violation (concurrent request) surfaces here
			entity = repository.saveAndFlush(entity);
		} catch (DataIntegrityViolationException e) {
			if (isCpfConstraintViolation(e)) {
				throw new DuplicateCpfException();
			}
			throw e;
		}
		return new DonorDTO(entity);
	}

	@Transactional
	public DonorDTO update(Long id, DonorRequestDTO dto) {
		Donor entity = repository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Doador não encontrado."));
		if (repository.existsByCpfAndIdNot(dto.cpf(), id)) {
			throw new DuplicateCpfException();
		}
		DonorDTO before = new DonorDTO(entity);
		copyDtoToEntity(dto, entity);
		// @PreUpdate only fires when the donor row changes; an address-only change would be missed
		if (!before.equals(new DonorDTO(entity))) {
			entity.setUpdatedAt(Instant.now());
		}
		try {
			entity = repository.saveAndFlush(entity);
		} catch (DataIntegrityViolationException e) {
			if (isCpfConstraintViolation(e)) {
				throw new DuplicateCpfException();
			}
			throw e;
		}
		return new DonorDTO(entity);
	}

	@Transactional
	public void delete(Long id) {
		// deleteById does not fail for a missing id, so look it up first (404)
		Donor entity = repository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Doador não encontrado."));
		repository.delete(entity);
	}

	private void copyDtoToEntity(DonorRequestDTO dto, Donor entity) {
		entity.setName(dto.name());
		entity.setCpf(dto.cpf());
		entity.setRg(blankToNull(dto.rg()));
		entity.setBirthDate(dto.birthDate());
		entity.setGender(Gender.fromId(dto.gender()).getId());
		MaritalStatus maritalStatus = MaritalStatus.fromId(dto.maritalStatus());
		entity.setMaritalStatus(maritalStatus == null ? null : maritalStatus.getId());
		BloodType bloodType = BloodType.fromId(dto.bloodType());
		entity.setBloodType(bloodType == null ? null : bloodType.getId());
		entity.setPhone(dto.phone());
		// Same scale as the NUMERIC(4,1) column, so 70 and 70.0 are the same value (@Digits allows 1 decimal)
		entity.setWeight(dto.weight() == null ? null : dto.weight().setScale(1));
		copyAddress(dto.address(), entity);
	}

	// Three cases: create, update the existing one in place, or remove (orphanRemoval).
	// Never replace an existing Address by a new object: Hibernate would INSERT the new row
	// before DELETE of the old one and break the unique donor_id.
	private void copyAddress(AddressRequestDTO dto, Donor entity) {
		if (dto == null) {
			entity.setAddress(null);
		} else if (entity.getAddress() == null) {
			Address address = new Address();
			copyAddressFields(dto, address);
			entity.setAddress(address);
		} else {
			copyAddressFields(dto, entity.getAddress());
		}
	}

	private void copyAddressFields(AddressRequestDTO dto, Address address) {
		address.setStreet(dto.street());
		address.setNumber(dto.number());
		address.setComplement(blankToNull(dto.complement()));
		address.setNeighborhood(dto.neighborhood());
		address.setZipCode(dto.zipCode());
		address.setCity(dto.city());
		address.setState(dto.state());
	}

	private String blankToNull(String value) {
		return value == null || value.isBlank() ? null : value;
	}

	private boolean isCpfConstraintViolation(Throwable error) {
		for (Throwable t = error; t != null; t = t.getCause()) {
			if (t.getMessage() != null && t.getMessage().toLowerCase().contains(CPF_CONSTRAINT)) {
				return true;
			}
		}
		return false;
	}
}
