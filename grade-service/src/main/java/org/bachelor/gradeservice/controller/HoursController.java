package org.bachelor.gradeservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.bachelor.gradeservice.model.dto.HoursDTO;
import org.bachelor.gradeservice.model.dto.HoursRequestDTO;
import org.bachelor.gradeservice.service.HoursService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/hours")
@RequiredArgsConstructor
public class HoursController {

    private final HoursService hoursService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public HoursDTO create(@Valid @RequestBody HoursRequestDTO dto) {
        return hoursService.create(dto);
    }

    @PutMapping("/{id}")
    public HoursDTO update(@PathVariable Long id,
                           @Valid @RequestBody HoursRequestDTO dto) {
        return hoursService.update(id, dto);
    }

    @GetMapping("/{id}")
    public HoursDTO getById(@PathVariable Long id) {
        return hoursService.getById(id);
    }

    @GetMapping("/by-discipline/{specialtyDisciplineId}")
    public HoursDTO getBySpecialtyDiscipline(@PathVariable Long specialtyDisciplineId) {
        return hoursService.getBySpecialtyDiscipline(specialtyDisciplineId);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        hoursService.delete(id);
    }
}
