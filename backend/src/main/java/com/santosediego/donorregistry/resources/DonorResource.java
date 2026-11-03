package com.santosediego.donorregistry.resources;

import java.net.URI;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.santosediego.donorregistry.dto.DonorDTO;
import com.santosediego.donorregistry.dto.DonorInsertDTO;
import com.santosediego.donorregistry.services.DonorService;

import jakarta.validation.Valid;

@RestController
@RequestMapping(value = "/donors")
public class DonorResource {

	private final DonorService service;

	public DonorResource(DonorService service) {
		this.service = service;
	}

	@GetMapping(value = "/{id}")
	public ResponseEntity<DonorDTO> findById(@PathVariable Long id) {
		return ResponseEntity.ok().body(service.findById(id));
	}

	@PostMapping
	public ResponseEntity<DonorDTO> insert(@Valid @RequestBody DonorInsertDTO dto) {
		DonorDTO created = service.insert(dto);
		URI uri = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(created.id())
				.toUri();
		return ResponseEntity.created(uri).body(created);
	}
}
