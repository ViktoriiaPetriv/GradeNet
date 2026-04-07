package org.bachelor.gradeservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.bachelor.gradeservice.model.dto.GradeDTO;
import org.bachelor.gradeservice.model.dto.GradeRequestDTO;
import org.bachelor.gradeservice.service.GradeService;
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

import java.util.List;

@RestController
@RequestMapping("/api/grades")
@RequiredArgsConstructor
public class GradeController {

    private final GradeService gradeService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public GradeDTO create(@Valid @RequestBody GradeRequestDTO dto) {
        return gradeService.create(dto);
    }

    @PutMapping("/{id}")
    public GradeDTO update(@PathVariable Long id,
                           @Valid @RequestBody GradeRequestDTO dto) {
        return gradeService.update(id, dto);
    }

    @GetMapping("/{id}")
    public GradeDTO getById(@PathVariable Long id) {
        return gradeService.getById(id);
    }

    @GetMapping
    public List<GradeDTO> getAll(
            @RequestParam(required = false) Long specialtyDisciplineId,
            @RequestParam(required = false) Long studentId) {
        if (specialtyDisciplineId != null) {
            return gradeService.getAllBySpecialtyDiscipline(specialtyDisciplineId);
        }
        if (studentId != null) {
            return gradeService.getAllByStudent(studentId);
        }
        return List.of();
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        gradeService.delete(id);
    }
}
