package org.bachelor.userservice.config;

import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

@Component
@RequiredArgsConstructor
public class OrgServiceClient {

    private final RestClient restClient;

    public List<Long> getSpecialtyIdsByOrgIds(List<Long> orgIds) {
        return restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/specialties/ids-by-orgs")
                        .queryParam("orgIds", orgIds)
                        .build())
                .retrieve()
                .onStatus(HttpStatusCode::isError, (req, res) -> {
                    throw new RuntimeException("org-service error: " + res.getStatusCode());
                })
                .body(new ParameterizedTypeReference<>() {});
    }

    public List<Long> getOfferingIdsBySpecialtyIds(List<Long> specialtyIds) {
        return restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/specialty-offerings/ids-by-specialties")
                        .queryParam("specialtyIds", specialtyIds)
                        .build())
                .retrieve()
                .onStatus(HttpStatusCode::isError, (req, res) -> {
                    throw new RuntimeException("org-service error: " + res.getStatusCode());
                })
                .body(new ParameterizedTypeReference<>() {});
    }

    public Long getSpecialtyIdByOfferingId(Long offeringId) {
        record OfferingInfo(Long specialtyId) {}
        return restClient.get()
                .uri("/api/specialty-offerings/{id}", offeringId)
                .retrieve()
                .onStatus(HttpStatusCode::isError, (req, res) -> {
                    throw new RuntimeException("org-service error: " + res.getStatusCode());
                })
                .body(OfferingInfo.class)
                .specialtyId();
    }
}
