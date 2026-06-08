package org.bachelor.addservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.bachelor.addservice.model.dto.CourseWorkDetailsCreateDTO;
import org.bachelor.addservice.model.dto.CourseWorkDetailsDTO;
import org.bachelor.addservice.service.CourseWorkDetailsService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/works/{workId}/course-work")
@RequiredArgsConstructor
public class CourseWorkDetailsController {

    private final CourseWorkDetailsService courseWorkDetailsService;

    @GetMapping
    public CourseWorkDetailsDTO get(@PathVariable Long workId) {
        return courseWorkDetailsService.getByAdditionalWorkId(workId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CourseWorkDetailsDTO create(@PathVariable Long workId,
                                       @RequestBody @Valid CourseWorkDetailsCreateDTO dto) {
        return courseWorkDetailsService.create(workId, dto);
    }

    @PutMapping
    public CourseWorkDetailsDTO update(@PathVariable Long workId,
                                       @RequestBody @Valid CourseWorkDetailsCreateDTO dto) {
        return courseWorkDetailsService.updateByWorkId(workId, dto);
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long workId) {
        courseWorkDetailsService.deleteByWorkId(workId);
    }
}
