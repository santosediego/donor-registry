package com.santosediego.donorregistry.repositories;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.santosediego.donorregistry.dto.DonorSummaryDTO;
import com.santosediego.donorregistry.entities.Donor;

@Repository
public interface DonorRepository extends JpaRepository<Donor, Long> {

	boolean existsByCpf(String cpf);

	boolean existsByCpfAndIdNot(String cpf, Long id);

	/*
	 * LEFT JOIN (not INNER) so a donor without address still shows up, and a direct
	 * projection so no Address entity is loaded (no N+1). The filter and its
	 * parameters are in DonorSearchQueries.
	 */
	@Query(value = "SELECT new com.santosediego.donorregistry.dto.DonorSummaryDTO("
			+ "d.id, d.name, d.phone, d.cpf, d.rg, d.bloodType, a.city) "
			+ "FROM Donor d LEFT JOIN d.address a" + DonorSearchQueries.WHERE,
			countQuery = "SELECT COUNT(d) FROM Donor d" + DonorSearchQueries.WHERE)
	Page<DonorSummaryDTO> search(@Param("filtered") boolean filtered, @Param("name") String name,
			@Param("cpfSearch") boolean cpfSearch, @Param("cpf") String cpf, @Param("rg") String rg,
			Pageable pageable);
}
