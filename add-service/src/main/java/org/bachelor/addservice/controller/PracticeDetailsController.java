package org.bachelor.addservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.bachelor.addservice.model.dto.PracticeDetailsCreateDTO;
import org.bachelor.addservice.model.dto.PracticeDetailsDTO;
import org.bachelor.addservice.service.PracticeDetailsService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/works/{workId}/practice")
@RequiredArgsConstructor
public class PracticeDetailsController {

    private final PracticeDetailsService practiceDetailsService;

    @GetMapping
    public PracticeDetailsDTO get(@PathVariable Long workId) {
        return practiceDetailsService.getByAdditionalWorkId(workId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PracticeDetailsDTO create(@PathVariable Long workId,
                                     @RequestBody @Valid PracticeDetailsCreateDTO dto) {
        return practiceDetailsService.create(workId, dto);
    }

    @PutMapping
    public PracticeDetailsDTO update(@PathVariable Long workId,
                                     @RequestBody @Valid PracticeDetailsCreateDTO dto) {
        return practiceDetailsService.updateByWorkId(workId, dto);
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long workId) {
        practiceDetailsService.deleteByWorkId(workId);
    }
}
