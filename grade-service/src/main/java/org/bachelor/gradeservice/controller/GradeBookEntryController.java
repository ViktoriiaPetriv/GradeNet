package org.bachelor.gradeservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.bachelor.gradeservice.model.dto.*;
import org.bachelor.gradeservice.model.entity.EntryResult;
import org.bachelor.gradeservice.service.GradeBookEntryService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/records")
@RequiredArgsConstructor
public class GradeBookEntryController {

    private final GradeBookEntryService entryService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public List<GradeBookEntryDTO> create(@RequestBody @Valid GradeBookEntryCreateDTO dto) {
        return entryService.create(dto);
    }

    // Повторна спроба після FAILED
    @PostMapping("/{id}/retake")
    @ResponseStatus(HttpStatus.CREATED)
    public GradeBookEntryDTO retake(@PathVariable Long id, @RequestParam Long professorId) {
        return entryService.retake(id, professorId);
    }

    // Закрити запис: PATCH /api/records/1/close?result=PASSED
    @PatchMapping("/close")
    public CloseEntryResponse close(@RequestBody @Valid CloseEntryDTO closeEntryDTO) {
        return entryService.closeAll(closeEntryDTO);
    }

    @GetMapping("/{id}")
    public GradeBookEntryDTO getById(@PathVariable Long id) {
        return entryService.getById(id);
    }

    @GetMapping
    public List<GradeBookEntryDTO> getAll(@ModelAttribute GradeBookEntryFilter filter) {
        return entryService.getAll(filter);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        entryService.delete(id);
    }

    @GetMapping("/students/{bookNumberId}/disciplines")
    public List<StudentDisciplineDTO> getStudentDisciplines(
            @PathVariable Long bookNumberId,
            @RequestParam(required = false) List<String> academicYears) {
        return entryService.getStudentDisciplines(bookNumberId,
                new StudentDisciplineFilter(academicYears));
    }

    @GetMapping("/bulk-view")
    public List<BulkGradeEntryDTO> getBulkEntries(
            @RequestParam Long specialtyDisciplineId,
            @RequestParam String academicYear) {
        return entryService.getBulkEntries(specialtyDisciplineId, academicYear);
    }

    @GetMapping("/group-report")
    public List<BulkGradeEntryDTO> getGroupReport(
            @RequestParam Long specialtyDisciplineId,
            @RequestParam String academicYear) {
        return entryService.getGroupReport(specialtyDisciplineId, academicYear);
    }
}
