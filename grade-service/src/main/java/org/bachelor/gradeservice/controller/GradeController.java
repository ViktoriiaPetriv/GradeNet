package org.bachelor.gradeservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.bachelor.gradeservice.model.dto.AuthenticatedUser;
import org.bachelor.gradeservice.model.dto.BulkGradeCreateDTO;
import org.bachelor.gradeservice.model.dto.GradeCreateDTO;
import org.bachelor.gradeservice.model.dto.GradeDTO;
import org.bachelor.gradeservice.model.dto.GradeUpdateDTO;
import org.bachelor.gradeservice.service.GradeService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/grades")
@RequiredArgsConstructor
public class GradeController {

    private final GradeService gradeService;

    @GetMapping
    public List<GradeDTO> getByEntryId(@RequestParam Long entryId) {
        return gradeService.getByEntryId(entryId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public GradeDTO create(@RequestBody @Valid GradeCreateDTO dto,
                           @AuthenticationPrincipal AuthenticatedUser user) {
        return gradeService.create(dto, user);
    }

    @PostMapping("/bulk")
    @ResponseStatus(HttpStatus.CREATED)
    public List<GradeDTO> createBulk(@RequestBody @Valid BulkGradeCreateDTO dto,
                                     @AuthenticationPrincipal AuthenticatedUser user) {
        return gradeService.createBulk(dto, user);
    }

    @PutMapping("/{id}")
    public GradeDTO update(@PathVariable Long id,
                           @RequestBody @Valid GradeUpdateDTO dto,
                           @AuthenticationPrincipal AuthenticatedUser user) {
        return gradeService.update(id, dto, user);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id,
                       @AuthenticationPrincipal AuthenticatedUser user) {
        gradeService.delete(id, user);
    }
}
