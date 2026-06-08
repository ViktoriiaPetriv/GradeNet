package org.bachelor.addservice.service;

import org.bachelor.addservice.model.dto.CourseWorkDetailsCreateDTO;
import org.bachelor.addservice.model.dto.CourseWorkDetailsDTO;

public interface CourseWorkDetailsService {
    CourseWorkDetailsDTO getByAdditionalWorkId(Long additionalWorkId);
    CourseWorkDetailsDTO create(Long additionalWorkId, CourseWorkDetailsCreateDTO dto);
    CourseWorkDetailsDTO updateByWorkId(Long additionalWorkId, CourseWorkDetailsCreateDTO dto);
    void deleteByWorkId(Long additionalWorkId);
}
