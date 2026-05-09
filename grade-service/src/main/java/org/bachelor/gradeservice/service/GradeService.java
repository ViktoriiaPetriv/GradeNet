package org.bachelor.gradeservice.service;

import org.bachelor.gradeservice.model.dto.AuthenticatedUser;
import org.bachelor.gradeservice.model.dto.BulkGradeCreateDTO;
import org.bachelor.gradeservice.model.dto.GradeCreateDTO;
import org.bachelor.gradeservice.model.dto.GradeDTO;
import org.bachelor.gradeservice.model.dto.GradeUpdateDTO;

import java.util.List;

public interface GradeService {
    GradeDTO create(GradeCreateDTO dto, AuthenticatedUser user);
    List<GradeDTO> createBulk(BulkGradeCreateDTO dto, AuthenticatedUser user);
    GradeDTO update(Long id, GradeUpdateDTO dto, AuthenticatedUser user);
    void delete(Long id, AuthenticatedUser user);
    List<GradeDTO> getByEntryId(Long entryId);
}
