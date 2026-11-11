package com.santosediego.donorregistry.support;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import org.springframework.jdbc.core.JdbcTemplate;

import com.santosediego.donorregistry.entities.Address;
import com.santosediego.donorregistry.entities.Donor;
import com.santosediego.donorregistry.repositories.DonorRepository;

/**
 * Prepares (Arrange) and inspects (Assert) the database in PUT/DELETE tests.
 * Plain class, built by the test with the beans it already receives. All data is fictitious.
 */
public final class DonorDatabase {

	/**
	 * Known old value for updated_at. A donor saved with it proves "updatedAt stayed as it was":
	 * no call to Instant.now() could ever produce this value. @PreUpdate does not run on INSERT,
	 * so setting it before the first save is kept.
	 */
	public static final Instant OLD_UPDATED_AT = Instant.parse("2020-01-01T10:00:00Z");

	private final DonorRepository repository;
	private final JdbcTemplate jdbc;

	public DonorDatabase(DonorRepository repository, JdbcTemplate jdbc) {
		this.repository = repository;
		this.jdbc = jdbc;
	}

	/** Saves a donor with every field filled and no address; updatedAt is OLD_UPDATED_AT. */
	public Donor saveDonorWithoutAddress(String cpf) {
		return repository.save(fullDonor(cpf));
	}

	/** Saves a donor with every field filled and an address (complement "Fundos"). */
	public Donor saveDonorWithAddress(String cpf) {
		return saveDonorWithAddress(cpf, "Fundos");
	}

	public Donor saveDonorWithAddress(String cpf, String complement) {
		Donor donor = fullDonor(cpf);
		Address address = new Address();
		address.setStreet("Rua Um");
		address.setNumber("10");
		address.setComplement(complement);
		address.setNeighborhood("Jardim");
		address.setZipCode("30140071");
		address.setCity("Belo Horizonte");
		address.setState("MG");
		donor.setAddress(address);
		return repository.save(donor);
	}

	/** Unsaved donor, every field filled, no address. Tests may change fields before saving. */
	public Donor fullDonor(String cpf) {
		Donor donor = new Donor();
		donor.setName("Fox Mulder");
		donor.setCpf(cpf);
		donor.setRg("MG-11.222.333");
		donor.setBirthDate(LocalDate.of(1980, 1, 15));
		donor.setGender("M");
		donor.setMaritalStatus("CAS");
		donor.setBloodType("O+");
		donor.setPhone("21999998888");
		donor.setWeight(new BigDecimal("70.0"));
		donor.setUpdatedAt(OLD_UPDATED_AT);
		return donor;
	}

	/** Number of rows in tb_address for this donor (0 or 1: donor_id is unique). */
	public int addressRows(Long donorId) {
		return jdbc.queryForObject("SELECT COUNT(*) FROM tb_address WHERE donor_id = ?", Integer.class, donorId);
	}

	/** Number of rows in tb_address with this id. */
	public int addressRowsWithId(Long addressId) {
		return jdbc.queryForObject("SELECT COUNT(*) FROM tb_address WHERE id = ?", Integer.class, addressId);
	}
}
