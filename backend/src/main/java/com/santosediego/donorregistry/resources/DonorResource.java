package com.santosediego.donorregistry.resources;

import java.net.URI;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedModel;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.santosediego.donorregistry.dto.DonorDTO;
import com.santosediego.donorregistry.dto.DonorRequestDTO;
import com.santosediego.donorregistry.dto.DonorSummaryDTO;
import com.santosediego.donorregistry.services.DonorService;

import jakarta.validation.Valid;

@RestController
@RequestMapping(value = "/donors")
public class DonorResource {

	private final DonorService service;

	public DonorResource(DonorService service) {
		this.service = service;
	}

	// page (from 0), size and sort come from Pageable; limits are in application.properties,
	// the default sort (name) is applied by the service
	@GetMapping
	public ResponseEntity<PagedModel<DonorSummaryDTO>> findAllPaged(
			@RequestParam(value = "search", required = false) String search,
			Pageable pageable) {
		Page<DonorSummaryDTO> page = service.findAllPaged(search, pageable);
		return ResponseEntity.ok().body(new PagedModel<>(page));
	}

	@GetMapping(value = "/{id}")
	public ResponseEntity<DonorDTO> findById(@PathVariable Long id) {
		return ResponseEntity.ok().body(service.findById(id));
	}

	@PostMapping
	public ResponseEntity<DonorDTO> insert(@Valid @RequestBody DonorRequestDTO dto) {
		DonorDTO created = service.insert(dto);
		URI uri = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(created.id())
				.toUri();
		return ResponseEntity.created(uri).body(created);
	}

	@PutMapping(value = "/{id}")
	public ResponseEntity<DonorDTO> update(@PathVariable Long id, @Valid @RequestBody DonorRequestDTO dto) {
		return ResponseEntity.ok().body(service.update(id, dto));
	}
}
