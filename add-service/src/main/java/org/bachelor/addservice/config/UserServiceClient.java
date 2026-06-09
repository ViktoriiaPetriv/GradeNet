package org.bachelor.addservice.config;

import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

@Component
@RequiredArgsConstructor
public class UserServiceClient {

    private final RestClient userServiceRestClient;

    public List<Long> getManagerBookNumberIds() {
        return userServiceRestClient.get()
                .uri("/api/books/allowed-ids")
                .retrieve()
                .onStatus(HttpStatusCode::isError, (req, res) -> {
                    throw new RuntimeException("user-service error: " + res.getStatusCode());
                })
                .body(new ParameterizedTypeReference<>() {});
    }
}
