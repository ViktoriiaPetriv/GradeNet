package org.bachelor.addservice.service;

import org.bachelor.addservice.model.dto.QualificationDetailsCreateDTO;
import org.bachelor.addservice.model.dto.QualificationDetailsDTO;

public interface QualificationDetailsService {
    QualificationDetailsDTO getByAdditionalWorkId(Long additionalWorkId);
    QualificationDetailsDTO create(Long additionalWorkId, QualificationDetailsCreateDTO dto);
    QualificationDetailsDTO updateByWorkId(Long additionalWorkId, QualificationDetailsCreateDTO dto);
    void deleteByWorkId(Long additionalWorkId);
}
