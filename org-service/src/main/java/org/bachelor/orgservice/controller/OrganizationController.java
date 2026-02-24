package org.bachelor.orgservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.bachelor.orgservice.model.dto.OrganizationDTO;
import org.bachelor.orgservice.model.dto.OrganizationRequestDTO;
import org.bachelor.orgservice.model.dto.OrganizationShortDTO;
import org.bachelor.orgservice.model.dto.PageResponse;
import org.bachelor.orgservice.model.entity.OrgType;
import org.bachelor.orgservice.service.OrganizationService;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@PreAuthorize("hasRole('ADMIN')")
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
    public PageResponse<OrganizationDTO> getAll(
            @RequestParam(required = false) OrgType orgType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return organizationService.getAll(orgType, PageRequest.of(page, size));
    }

    @GetMapping("/short")
    public List<OrganizationShortDTO> getAllShort(
            @RequestParam(required = false) OrgType orgType
    ) {
        return organizationService.getAllShort(orgType);
    }
}
