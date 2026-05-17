package org.bachelor.orgservice.config;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@RequiredArgsConstructor
public class GradeServiceClient {

    private final RestClient gradeServiceRestClient;

    public boolean hasSpecialtyDisciplines(Long specialtyOfferingId) {
        Boolean result = gradeServiceRestClient.get()
                .uri("/api/specialty-disciplines/exists?specialtyOfferingId={id}", specialtyOfferingId)
                .retrieve()
                .body(Boolean.class);
        return Boolean.TRUE.equals(result);
    }
}
