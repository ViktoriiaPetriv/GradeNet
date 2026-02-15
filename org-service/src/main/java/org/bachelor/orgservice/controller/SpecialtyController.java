package org.bachelor.orgservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.bachelor.orgservice.model.dto.PageResponse;
import org.bachelor.orgservice.model.dto.SpecialtyDTO;
import org.bachelor.orgservice.model.dto.SpecialtyRequestDTO;
import org.bachelor.orgservice.model.entity.Degree;
import org.bachelor.orgservice.model.entity.EduType;
import org.bachelor.orgservice.service.SpecialtyService;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/specialties")
public class SpecialtyController {

    private final SpecialtyService specialtyService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SpecialtyDTO create(@Valid @RequestBody SpecialtyRequestDTO dto) {
        return specialtyService.create(dto);
    }

    @PutMapping("/{id}")
    public SpecialtyDTO update(@PathVariable Long id,
                               @Valid @RequestBody SpecialtyRequestDTO dto) {
        return specialtyService.update(id, dto);
    }

    @GetMapping("/{id}")
    public SpecialtyDTO getById(@PathVariable Long id) {
        return specialtyService.getById(id);
    }

    @GetMapping
    public PageResponse<SpecialtyDTO> getAll(
            @RequestParam(required = false) Degree degree,
            @RequestParam(required = false) EduType eduType,
            @PageableDefault(size = 20, sort = "id") Pageable pageable) {
        return specialtyService.getAll(degree, eduType, pageable);
    }

    @GetMapping("/organization/{orgId}")
    public PageResponse<SpecialtyDTO> getAllByOrganization(
            @PathVariable Long orgId,
            @RequestParam(required = false) Degree degree,
            @RequestParam(required = false) EduType eduType,
            @PageableDefault(size = 20, sort = "id") Pageable pageable) {
        return specialtyService.getAllByOrganization(orgId, degree, eduType, pageable);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        specialtyService.delete(id);
    }
}
