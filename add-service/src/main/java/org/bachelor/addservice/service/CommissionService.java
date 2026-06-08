package org.bachelor.addservice.service;

import org.bachelor.addservice.model.dto.CommissionCreateDTO;
import org.bachelor.addservice.model.dto.CommissionDTO;
import org.bachelor.addservice.model.dto.CommissionMemberCreateDTO;
import org.bachelor.addservice.model.dto.CommissionMemberDTO;

import java.util.List;

public interface CommissionService {
    List<CommissionDTO> getAll();
    CommissionDTO getById(Long id);
    CommissionDTO create(CommissionCreateDTO dto, Long orgId);
    CommissionDTO update(Long id, CommissionCreateDTO dto);
    void delete(Long id);
    CommissionMemberDTO addMember(Long commissionId, CommissionMemberCreateDTO dto);
    void removeMember(Long commissionId, Long memberId);
}
