package org.bachelor.integrationservice.service.journal;

import org.bachelor.integrationservice.model.journal.JournalDisciplineDTO;
import org.bachelor.integrationservice.model.journal.JournalDisciplineDetailDTO;
import org.bachelor.integrationservice.model.journal.JournalStudentDTO;

import java.util.List;

public interface JournalClient {

    List<Long> getSpecialties(String degree, Integer graduationYear, String studyForm, String code);

    List<JournalDisciplineDTO> getDisciplines(long specialtyId);

    List<JournalStudentDTO> getStudents(long specialtyId);

    JournalDisciplineDetailDTO getDisciplineDetail(long disciplineId);
}
