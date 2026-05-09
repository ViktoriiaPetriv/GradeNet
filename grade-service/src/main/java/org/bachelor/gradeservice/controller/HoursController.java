package org.bachelor.gradeservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.bachelor.gradeservice.model.dto.HoursCreateDTO;
import org.bachelor.gradeservice.model.dto.HoursDTO;
import org.bachelor.gradeservice.service.HoursService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/hours")
@RequiredArgsConstructor
public class HoursController {

    private final HoursService hoursService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public HoursDTO addHours(@RequestParam Long specialtyDisciplineId,
                             @RequestBody @Valid HoursCreateDTO dto) {
        return hoursService.addHours(specialtyDisciplineId, dto);
    }

    @GetMapping("/{id}")
    public HoursDTO getById(@PathVariable Long id) {
        return hoursService.getById(id);
    }

    @GetMapping
    public List<HoursDTO> getAll(@RequestParam Long specialtyDisciplineId) {
        return hoursService.getAll(specialtyDisciplineId);
    }

    @PutMapping("/{id}")
    public HoursDTO updateHours(@PathVariable Long id,
                                @RequestBody @Valid HoursCreateDTO dto) {
        return hoursService.updateHours(id, dto);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteHours(@PathVariable Long id) {
        hoursService.deleteHours(id);
    }
}
