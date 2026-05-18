package org.bachelor.integrationservice.service.journal;

import lombok.RequiredArgsConstructor;
import org.bachelor.integrationservice.model.journal.JournalDisciplineDTO;
import org.bachelor.integrationservice.model.journal.JournalDisciplineDetailDTO;
import org.bachelor.integrationservice.model.journal.JournalStudentDTO;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;

@RequiredArgsConstructor
public class HttpJournalClient implements JournalClient {

    private final RestClient restClient;
    private final String baseUrl;

    @Override
    public List<Long> getSpecialties(String degree, Integer graduationYear, String studyForm, String code) {
        var uri = UriComponentsBuilder.fromUriString(baseUrl + "/specialties")
                .queryParamIfPresent("degree", java.util.Optional.ofNullable(degree))
                .queryParamIfPresent("graduationYear", java.util.Optional.ofNullable(graduationYear))
                .queryParamIfPresent("studyForm", java.util.Optional.ofNullable(studyForm))
                .queryParamIfPresent("code", java.util.Optional.ofNullable(code))
                .build().toUri();
        return restClient.get().uri(uri).retrieve().body(new ParameterizedTypeReference<>() {});
    }

    @Override
    public List<JournalDisciplineDTO> getDisciplines(long specialtyId) {
        return restClient.get()
                .uri(baseUrl + "/disciplines/{id}", specialtyId)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
    }

    @Override
    public List<JournalStudentDTO> getStudents(long specialtyId) {
        return restClient.get()
                .uri(baseUrl + "/students/{id}", specialtyId)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
    }

    @Override
    public JournalDisciplineDetailDTO getDisciplineDetail(long disciplineId) {
        return restClient.get()
                .uri(baseUrl + "/discipline/{id}", disciplineId)
                .retrieve()
                .body(JournalDisciplineDetailDTO.class);
    }
}
