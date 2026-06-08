package org.bachelor.addservice.service;

import org.bachelor.addservice.model.dto.AdditionalWorkCreateDTO;
import org.bachelor.addservice.model.dto.AdditionalWorkDTO;
import org.bachelor.addservice.model.dto.AuthenticatedUser;
import org.bachelor.addservice.model.dto.GradeWorkDTO;

import java.util.List;

public interface AdditionalWorkService {
    List<AdditionalWorkDTO> getAll();
    AdditionalWorkDTO getById(Long id);
    List<AdditionalWorkDTO> getByCommissionId(Long commissionId);
    List<AdditionalWorkDTO> getByBookNumberId(Long bookNumberId);
    AdditionalWorkDTO create(AdditionalWorkCreateDTO dto);
    AdditionalWorkDTO update(Long id, AdditionalWorkCreateDTO dto);
    AdditionalWorkDTO grade(Long id, GradeWorkDTO dto, AuthenticatedUser user);
    void delete(Long id);
}
