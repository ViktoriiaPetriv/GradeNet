package org.bachelor.gradeservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.bachelor.gradeservice.model.dto.*;
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
    public DisciplineCreateResponseDTO create(@RequestBody @Valid DisciplineCreateDTO dto) {
        return disciplineService.create(dto);
    }

    @GetMapping
    public List<DisciplineDTO> getAll() {
        return disciplineService.getAll();
    }

    @GetMapping("/{id}")
    public DisciplineDTO getById(@PathVariable Long id) {
        return disciplineService.getById(id);
    }

    @PutMapping("/{id}")
    public DisciplineDTO update(@PathVariable Long id,
                                @RequestBody @Valid DisciplineUpdateDTO dto) {
        return disciplineService.update(id, dto);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        disciplineService.delete(id);
    }
}
