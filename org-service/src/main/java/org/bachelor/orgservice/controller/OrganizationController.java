package org.bachelor.orgservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.bachelor.orgservice.model.dto.OrganizationDTO;
import org.bachelor.orgservice.model.dto.OrganizationRequestDTO;
import org.bachelor.orgservice.service.OrganizationService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/orgs")
public class OrganizationController {

    private final OrganizationService organizationService;

    @PostMapping
    public OrganizationDTO create(@Valid @RequestBody OrganizationRequestDTO dto) {
        return organizationService.create(dto);
    }

    @PutMapping("/{id}")
    public OrganizationDTO update(
            @PathVariable Long id,
            @Valid @RequestBody OrganizationRequestDTO dto
    ) {
        return organizationService.update(id, dto);
    }

    @GetMapping("/{id}")
    public OrganizationDTO getById(@PathVariable Long id) {
        return organizationService.getById(id);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        organizationService.delete(id);
    }

    @GetMapping
    public List<OrganizationDTO> getAll() {
        return organizationService.getAll();
    }
}
