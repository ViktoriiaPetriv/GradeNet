package org.bachelor.gradeservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.bachelor.gradeservice.model.dto.DisciplineDTO;
import org.bachelor.gradeservice.model.dto.DisciplineRequestDTO;
import org.bachelor.gradeservice.service.DisciplineService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/disciplines")
@RequiredArgsConstructor
public class DisciplineController {

    private final DisciplineService disciplineService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DisciplineDTO create(@Valid @RequestBody DisciplineRequestDTO dto) {
        return disciplineService.create(dto);
    }

    @PutMapping("/{id}")
    public DisciplineDTO update(@PathVariable Long id,
                                @Valid @RequestBody DisciplineRequestDTO dto) {
        return disciplineService.update(id, dto);
    }

    @GetMapping("/{id}")
    public DisciplineDTO getById(@PathVariable Long id) {
        return disciplineService.getById(id);
    }

    @GetMapping
    public List<DisciplineDTO> getAll() {
        return disciplineService.getAll();
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        disciplineService.delete(id);
    }
}
