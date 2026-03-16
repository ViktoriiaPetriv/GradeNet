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
}
