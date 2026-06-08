package org.bachelor.addservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.bachelor.addservice.model.dto.AuthenticatedUser;
import org.bachelor.addservice.model.dto.CommissionCreateDTO;
import org.bachelor.addservice.model.dto.CommissionDTO;
import org.bachelor.addservice.model.dto.CommissionMemberCreateDTO;
import org.bachelor.addservice.model.dto.CommissionMemberDTO;
import org.bachelor.addservice.service.CommissionService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/commissions")
@RequiredArgsConstructor
public class CommissionController {

    private final CommissionService commissionService;

    @GetMapping
    public List<CommissionDTO> getAll() {
        return commissionService.getAll();
    }

    @GetMapping("/{id}")
    public CommissionDTO getById(@PathVariable Long id) {
        return commissionService.getById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CommissionDTO create(@RequestBody @Valid CommissionCreateDTO dto,
                                @AuthenticationPrincipal AuthenticatedUser user) {
        return commissionService.create(dto, user.orgId());
    }

    @PutMapping("/{id}")
    public CommissionDTO update(@PathVariable Long id,
                                @RequestBody @Valid CommissionCreateDTO dto) {
        return commissionService.update(id, dto);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        commissionService.delete(id);
    }

    @PostMapping("/{commissionId}/members")
    @ResponseStatus(HttpStatus.CREATED)
    public CommissionMemberDTO addMember(@PathVariable Long commissionId,
                                         @RequestBody @Valid CommissionMemberCreateDTO dto) {
        return commissionService.addMember(commissionId, dto);
    }

    @DeleteMapping("/{commissionId}/members/{memberId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeMember(@PathVariable Long commissionId,
                             @PathVariable Long memberId) {
        commissionService.removeMember(commissionId, memberId);
    }
}
