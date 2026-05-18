package org.bachelor.orgservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.bachelor.orgservice.model.dto.SpecialtyOfferingDTO;
import org.bachelor.orgservice.model.dto.SpecialtyOfferingRequestDTO;
import org.bachelor.orgservice.service.SpecialtyOfferingService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/specialty-offerings")
public class SpecialtyOfferingController {

    private final SpecialtyOfferingService specialtyOfferingService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SpecialtyOfferingDTO create(@Valid @RequestBody SpecialtyOfferingRequestDTO request) {
        return specialtyOfferingService.create(request);
    }

    @GetMapping("/{id}")
    public SpecialtyOfferingDTO getById(@PathVariable Long id) {
        return specialtyOfferingService.getById(id);
    }

    @GetMapping
    public List<SpecialtyOfferingDTO> getAllBySpecialty(@RequestParam Long specialtyId) {
        return specialtyOfferingService.getAllBySpecialty(specialtyId);
    }

    @GetMapping("/by-external-id")
    public org.springframework.http.ResponseEntity<SpecialtyOfferingDTO> getByExternalId(
            @RequestParam Long externalId) {
        return specialtyOfferingService.getByExternalId(externalId)
                .map(org.springframework.http.ResponseEntity::ok)
                .orElse(org.springframework.http.ResponseEntity.notFound().build());
    }

    @GetMapping("/ids-by-specialties")
    public List<Long> getIdsBySpecialtyIds(@RequestParam List<Long> specialtyIds) {
        return specialtyOfferingService.getIdsBySpecialtyIds(specialtyIds);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        specialtyOfferingService.delete(id);
    }
}
