package org.bachelor.gradeservice.config;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

@Component
@RequiredArgsConstructor
public class OrgServiceClient {

    private final RestClient orgServiceRestClient;

    public boolean specialtyExists(Long specialtyId) {
        try {
            orgServiceRestClient.get()
                    .uri("/api/specialties/{id}", specialtyId)
                    .retrieve()
                    .toBodilessEntity();
            return true;
        } catch (HttpClientErrorException.NotFound e) {
            return false;
        }
    }
}
