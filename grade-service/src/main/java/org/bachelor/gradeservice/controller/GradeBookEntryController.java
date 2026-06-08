package org.bachelor.gradeservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.bachelor.gradeservice.model.dto.*;
import org.bachelor.gradeservice.service.GradeBookEntryService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

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
    public PageResponse<GradeBookEntryDTO> getAll(
            @ModelAttribute GradeBookEntryFilter filter,
            @RequestParam(defaultValue = "0") int pageNumber,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {

        Set<String> allowed = Set.of("id", "academicYear", "semester", "attempt",
                "status", "result", "reportDate", "specialtyDiscipline.discipline.name");
        String field = allowed.contains(sortBy) ? sortBy : "id";
        Pageable pageable = PageRequest.of(
                pageNumber, size,
                sortDir.equalsIgnoreCase("desc")
                        ? Sort.by(field).descending()
                        : Sort.by(field).ascending()
        );
        return entryService.getAll(filter, pageable);
    }

    @PatchMapping("/{id}/reset")
    public GradeBookEntryDTO reset(@PathVariable Long id) {
        return entryService.reset(id);
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

    @GetMapping("/students/{bookNumberId}/report")
    public StudentGradeReportDTO getStudentGradeReport(
            @PathVariable Long bookNumberId,
            @RequestParam(required = false) List<String> academicYears) {
        return entryService.getStudentGradeReport(bookNumberId,
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
