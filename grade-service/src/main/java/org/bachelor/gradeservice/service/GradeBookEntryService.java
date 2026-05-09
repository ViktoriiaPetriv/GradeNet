package org.bachelor.gradeservice.service;

import org.bachelor.gradeservice.model.dto.*;
import org.bachelor.gradeservice.model.entity.EntryResult;

import java.util.List;

public interface GradeBookEntryService {
    List<GradeBookEntryDTO> create(GradeBookEntryCreateDTO dto);
    GradeBookEntryDTO retake(Long entryId, Long professorId);
    CloseEntryResponse closeAll(CloseEntryDTO closeEntryDTO);
    GradeBookEntryDTO getById(Long id);
    List<GradeBookEntryDTO> getAll(GradeBookEntryFilter filter);
    void delete(Long id);
    List<StudentDisciplineDTO> getStudentDisciplines(Long bookNumberId, StudentDisciplineFilter filter);
    List<BulkGradeEntryDTO> getBulkEntries(Long specialtyDisciplineId, String academicYear);
    List<BulkGradeEntryDTO> getGroupReport(Long specialtyDisciplineId, String academicYear);
}
