package org.bachelor.gradeservice.service;

import org.bachelor.gradeservice.model.dto.HoursCreateDTO;
import org.bachelor.gradeservice.model.dto.HoursDTO;

import java.util.List;

public interface HoursService {
    HoursDTO addHours(Long specialtyDisciplineId, HoursCreateDTO dto);
    HoursDTO updateHours(Long hoursId, HoursCreateDTO dto);
    void deleteHours(Long hoursId);
    HoursDTO getById(Long id);
    List<HoursDTO> getAll(Long specialtyDisciplineId);
}
