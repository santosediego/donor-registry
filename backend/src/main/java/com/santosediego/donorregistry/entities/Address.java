package com.santosediego.donorregistry.entities;

import java.io.Serializable;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "tb_address")
public class Address implements Serializable {

	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "street", length = 150)
	private String street;

	@Column(name = "street_number", length = 10)
	private String number;

	@Column(name = "complement", length = 100)
	private String complement;

	@Column(name = "neighborhood", length = 80)
	private String neighborhood;

	// Digits only
	@Column(name = "zip_code", length = 8)
	private String zipCode;

	@Column(name = "city", length = 100)
	private String city;

	// UF (two letters)
	@Column(name = "state", length = 2)
	private String state;

	// Owning side of the 1:1 relation: the foreign key lives in tb_address.donor_id
	@OneToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "donor_id", nullable = false, unique = true)
	private Donor donor;

	public Address() {
	}

	public Address(Long id, String street, String number, String complement, String neighborhood,
			String zipCode, String city, String state, Donor donor) {
		this.id = id;
		this.street = street;
		this.number = number;
		this.complement = complement;
		this.neighborhood = neighborhood;
		this.zipCode = zipCode;
		this.city = city;
		this.state = state;
		this.donor = donor;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getStreet() {
		return street;
	}

	public void setStreet(String street) {
		this.street = street;
	}

	public String getNumber() {
		return number;
	}

	public void setNumber(String number) {
		this.number = number;
	}

	public String getComplement() {
		return complement;
	}

	public void setComplement(String complement) {
		this.complement = complement;
	}

	public String getNeighborhood() {
		return neighborhood;
	}

	public void setNeighborhood(String neighborhood) {
		this.neighborhood = neighborhood;
	}

	public String getZipCode() {
		return zipCode;
	}

	public void setZipCode(String zipCode) {
		this.zipCode = zipCode;
	}

	public String getCity() {
		return city;
	}

	public void setCity(String city) {
		this.city = city;
	}

	public String getState() {
		return state;
	}

	public void setState(String state) {
		this.state = state;
	}

	public Donor getDonor() {
		return donor;
	}

	public void setDonor(Donor donor) {
		this.donor = donor;
	}

	@Override
	public int hashCode() {
		return Objects.hash(id);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null || getClass() != obj.getClass()) {
			return false;
		}
		Address other = (Address) obj;
		return Objects.equals(id, other.id);
	}
}
