package com.santosediego.donorregistry.services;

import java.util.List;
import java.util.Locale;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.santosediego.donorregistry.dto.AddressInsertDTO;
import com.santosediego.donorregistry.dto.DonorDTO;
import com.santosediego.donorregistry.dto.DonorInsertDTO;
import com.santosediego.donorregistry.dto.DonorSummaryDTO;
import com.santosediego.donorregistry.entities.Address;
import com.santosediego.donorregistry.entities.Donor;
import com.santosediego.donorregistry.entities.enums.BloodType;
import com.santosediego.donorregistry.entities.enums.Gender;
import com.santosediego.donorregistry.entities.enums.MaritalStatus;
import com.santosediego.donorregistry.repositories.DonorRepository;
import com.santosediego.donorregistry.services.exceptions.DuplicateCpfException;
import com.santosediego.donorregistry.services.exceptions.InvalidSortException;
import com.santosediego.donorregistry.services.exceptions.ResourceNotFoundException;

@Service
public class DonorService {

	// Name of the unique constraint on tb_donor.cpf (see changeSet 001)
	private static final String CPF_CONSTRAINT = "uk_tb_donor_cpf";

	// Fields the list can be sorted by (property names of Donor)
	private static final List<String> SORTABLE_FIELDS = List.of("name", "createdAt");

	// Must match the ESCAPE character used in DonorRepository
	private static final char LIKE_ESCAPE = '!';

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
		return repository.search(true, likePattern(term), cpfSearch, cpfSearch ? likePattern(cpfDigits) : "",
				likePattern(term.toLowerCase(Locale.ROOT)), safe);
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

	// "%term%" with %, _ and the escape character taken literally
	private String likePattern(String term) {
		String escaped = term.replace(String.valueOf(LIKE_ESCAPE), "" + LIKE_ESCAPE + LIKE_ESCAPE)
				.replace("%", LIKE_ESCAPE + "%").replace("_", LIKE_ESCAPE + "_");
		return "%" + escaped + "%";
	}

	@Transactional
	public DonorDTO insert(DonorInsertDTO dto) {
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

	private void copyDtoToEntity(DonorInsertDTO dto, Donor entity) {
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
		entity.setWeight(dto.weight());
		entity.setAddress(dto.address() == null ? null : toAddress(dto.address()));
	}

	private Address toAddress(AddressInsertDTO dto) {
		Address address = new Address();
		address.setStreet(dto.street());
		address.setNumber(dto.number());
		address.setComplement(blankToNull(dto.complement()));
		address.setNeighborhood(dto.neighborhood());
		address.setZipCode(dto.zipCode());
		address.setCity(dto.city());
		address.setState(dto.state());
		return address;
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
