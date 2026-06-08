package org.bachelor.addservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.bachelor.addservice.model.dto.QualificationDetailsCreateDTO;
import org.bachelor.addservice.model.dto.QualificationDetailsDTO;
import org.bachelor.addservice.service.QualificationDetailsService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/works/{workId}/qualification")
@RequiredArgsConstructor
public class QualificationDetailsController {

    private final QualificationDetailsService qualificationDetailsService;

    @GetMapping
    public QualificationDetailsDTO get(@PathVariable Long workId) {
        return qualificationDetailsService.getByAdditionalWorkId(workId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public QualificationDetailsDTO create(@PathVariable Long workId,
                                           @RequestBody @Valid QualificationDetailsCreateDTO dto) {
        return qualificationDetailsService.create(workId, dto);
    }

    @PutMapping
    public QualificationDetailsDTO update(@PathVariable Long workId,
                                           @RequestBody @Valid QualificationDetailsCreateDTO dto) {
        return qualificationDetailsService.updateByWorkId(workId, dto);
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long workId) {
        qualificationDetailsService.deleteByWorkId(workId);
    }
}
