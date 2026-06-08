package org.bachelor.addservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.bachelor.addservice.model.dto.AdditionalWorkCreateDTO;
import org.bachelor.addservice.model.dto.AdditionalWorkDTO;
import org.bachelor.addservice.model.dto.AuthenticatedUser;
import org.bachelor.addservice.model.dto.GradeWorkDTO;
import org.bachelor.addservice.service.AdditionalWorkService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/works")
@RequiredArgsConstructor
public class AdditionalWorkController {

    private final AdditionalWorkService additionalWorkService;

    @GetMapping
    public List<AdditionalWorkDTO> getAll() {
        return additionalWorkService.getAll();
    }

    @GetMapping("/{id}")
    public AdditionalWorkDTO getById(@PathVariable Long id) {
        return additionalWorkService.getById(id);
    }

    @GetMapping("/by-commission/{commissionId}")
    public List<AdditionalWorkDTO> getByCommissionId(@PathVariable Long commissionId) {
        return additionalWorkService.getByCommissionId(commissionId);
    }

    @GetMapping("/by-book-number/{bookNumberId}")
    public List<AdditionalWorkDTO> getByBookNumberId(@PathVariable Long bookNumberId) {
        return additionalWorkService.getByBookNumberId(bookNumberId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AdditionalWorkDTO create(@RequestBody @Valid AdditionalWorkCreateDTO dto) {
        return additionalWorkService.create(dto);
    }

    @PutMapping("/{id}")
    public AdditionalWorkDTO update(@PathVariable Long id,
                                    @RequestBody @Valid AdditionalWorkCreateDTO dto) {
        return additionalWorkService.update(id, dto);
    }

    @PatchMapping("/{id}/grade")
    public AdditionalWorkDTO grade(@PathVariable Long id,
                                   @RequestBody GradeWorkDTO dto,
                                   @AuthenticationPrincipal AuthenticatedUser user) {
        return additionalWorkService.grade(id, dto, user);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        additionalWorkService.delete(id);
    }
}
