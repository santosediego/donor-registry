package com.santosediego.donorregistry.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.santosediego.donorregistry.entities.Donor;

@Repository
public interface DonorRepository extends JpaRepository<Donor, Long> {

	boolean existsByCpf(String cpf);
}
