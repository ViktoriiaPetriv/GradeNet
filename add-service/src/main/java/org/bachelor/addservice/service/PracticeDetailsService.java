package org.bachelor.addservice.service;

import org.bachelor.addservice.model.dto.PracticeDetailsCreateDTO;
import org.bachelor.addservice.model.dto.PracticeDetailsDTO;

public interface PracticeDetailsService {
    PracticeDetailsDTO getByAdditionalWorkId(Long additionalWorkId);
    PracticeDetailsDTO create(Long additionalWorkId, PracticeDetailsCreateDTO dto);
    PracticeDetailsDTO updateByWorkId(Long additionalWorkId, PracticeDetailsCreateDTO dto);
    void deleteByWorkId(Long additionalWorkId);
}
