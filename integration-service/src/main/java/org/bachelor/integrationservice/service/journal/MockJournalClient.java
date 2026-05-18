package org.bachelor.integrationservice.service.journal;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.bachelor.integrationservice.model.journal.JournalDisciplineDTO;
import org.bachelor.integrationservice.model.journal.JournalDisciplineDetailDTO;
import org.bachelor.integrationservice.model.journal.JournalSpecialtyDTO;
import org.bachelor.integrationservice.model.journal.JournalStudentDTO;

import java.util.stream.Collectors;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

@RequiredArgsConstructor
public class MockJournalClient implements JournalClient {

    private static final String BASE = "mock-journal/";

    private final ObjectMapper objectMapper;

    @Override
    public List<Long> getSpecialties(String degree, Integer graduationYear, String studyForm, String code) {
        List<JournalSpecialtyDTO> all = readJson("specialty/list.json", new TypeReference<>() {});
        return all.stream()
                .filter(s -> degree == null || degree.isBlank() || degree.equalsIgnoreCase(s.getDegree()))
                .filter(s -> graduationYear == null || graduationYear.equals(s.getGraduationYear()))
                .filter(s -> studyForm == null || studyForm.isBlank() || studyForm.equalsIgnoreCase(s.getStudyForm()))
                .filter(s -> code == null || code.isBlank() || code.equalsIgnoreCase(s.getCode()))
                .map(JournalSpecialtyDTO::getExternalId)
                .collect(Collectors.toList());
    }

    @Override
    public List<JournalDisciplineDTO> getDisciplines(long specialtyId) {
        return readJson("discipline/" + specialtyId + ".json", new TypeReference<>() {});
    }

    @Override
    public List<JournalStudentDTO> getStudents(long specialtyId) {
        return readJson("student/" + specialtyId + ".json", new TypeReference<>() {});
    }

    @Override
    public JournalDisciplineDetailDTO getDisciplineDetail(long disciplineId) {
        return readJson("grade/" + disciplineId + ".json", new TypeReference<>() {});
    }

    private <T> T readJson(String relativePath, TypeReference<T> type) {
        var resource = new ClassPathResource(BASE + relativePath);
        try (InputStream in = resource.getInputStream()) {
            return objectMapper.readValue(in, type);
        } catch (IOException e) {
            throw new IllegalStateException("Mock journal file not found: " + relativePath, e);
        }
    }
}
