package org.bachelor.userservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.bachelor.userservice.model.dto.PageResponse;
import org.bachelor.userservice.model.dto.StudentGroupDTO;
import org.bachelor.userservice.model.dto.StudentGroupMemberDTO;
import org.bachelor.userservice.model.dto.StudentGroupRequestDTO;
import org.bachelor.userservice.service.StudentGroupService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
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
@RequestMapping("/api/groups")
@RequiredArgsConstructor
public class StudentGroupController {

    private final StudentGroupService studentGroupService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'PROFESSOR')")
    public PageResponse<StudentGroupDTO> findAll(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Long specialtyId,
            @RequestParam(defaultValue = "0") int pageNumber,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "name") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {

        Pageable pageable = PageRequest.of(
                pageNumber, size,
                sortDir.equalsIgnoreCase("desc")
                        ? Sort.by(sortBy).descending()
                        : Sort.by(sortBy).ascending()
        );
        return studentGroupService.findAll(name, specialtyId, pageable);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public StudentGroupDTO findById(@PathVariable Long id) {
        return studentGroupService.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public StudentGroupDTO create(@Valid @RequestBody StudentGroupRequestDTO request) {
        return studentGroupService.create(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public StudentGroupDTO update(
            @PathVariable Long id,
            @Valid @RequestBody StudentGroupRequestDTO request) {
        return studentGroupService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    public void delete(@PathVariable Long id) {
        studentGroupService.delete(id);
    }

    @GetMapping("/{id}/members")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'PROFESSOR')")
    public List<StudentGroupMemberDTO> findMembers(@PathVariable Long id) {
        return studentGroupService.findMembers(id);
    }

    @PostMapping("/{id}/members/{bookNumberId}")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public StudentGroupMemberDTO addMember(
            @PathVariable Long id,
            @PathVariable Long bookNumberId) {
        return studentGroupService.addMember(id, bookNumberId);
    }

    @DeleteMapping("/{id}/members/{bookNumberId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public void removeMember(
            @PathVariable Long id,
            @PathVariable Long bookNumberId) {
        studentGroupService.removeMember(id, bookNumberId);
    }
}
