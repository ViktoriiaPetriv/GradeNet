package org.bachelor.gradeservice.controller;

import lombok.RequiredArgsConstructor;
import org.bachelor.gradeservice.model.dto.SpecialtyDisciplineDTO;
import org.bachelor.gradeservice.model.dto.SpecialtyDisciplineFilter;
import org.bachelor.gradeservice.service.SpecialtyDisciplineService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/specialty-disciplines")
@RequiredArgsConstructor
public class SpecialtyDisciplineController {
    private final SpecialtyDisciplineService specialtyDisciplineService;

    @PostMapping("/{id}")
    @ResponseStatus(HttpStatus.CREATED)
    public SpecialtyDisciplineDTO addSpecialty(@RequestParam Long disciplineId,
                                               @PathVariable Long id) {
        return specialtyDisciplineService.addSpecialty(disciplineId, id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeSpecialty(@RequestParam Long disciplineId,
                                @PathVariable Long id) {
        specialtyDisciplineService.removeSpecialty(disciplineId, id);
    }

    @GetMapping("/{id}")
    public SpecialtyDisciplineDTO getById(@PathVariable Long id) {
        return specialtyDisciplineService.getById(id);
    }

    @GetMapping
    public List<SpecialtyDisciplineDTO> getAll(@ModelAttribute SpecialtyDisciplineFilter filter) {
        return specialtyDisciplineService.getAll(filter);
    }

    @GetMapping("/exists")
    public boolean existsBySpecialtyOffering(@RequestParam Long specialtyOfferingId) {
        return specialtyDisciplineService.existsBySpecialtyOfferingId(specialtyOfferingId);
    }
}
