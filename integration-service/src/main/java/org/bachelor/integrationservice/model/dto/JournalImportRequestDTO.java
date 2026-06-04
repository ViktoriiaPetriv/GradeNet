package org.bachelor.integrationservice.model.dto;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class JournalImportRequestDTO {

    private Long journalSpecialtyId;
    private Long specialtyOfferingId;
    private String academicYear;

    /** Key = journal discipline external_id, Value = map of (attempt → professorId). */
    private Map<Long, Map<Integer, Long>> professorByDisciplineId;

    /** Per-student professor overrides: discipline → attempt → studentExternalId → professorId. */
    private Map<Long, Map<Integer, Map<Long, Long>>> professorOverridesByStudent;

    /** Journal student external IDs to include; null means all students. */
    private List<Long> selectedStudentExternalIds;
}
