package org.bachelor.gradeservice.config;

import lombok.RequiredArgsConstructor;
import org.bachelor.gradeservice.model.dto.UserDTO;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

@Component
@RequiredArgsConstructor
public class UserServiceClient {

    private final RestClient userServiceRestClient;

    public boolean professorExists(Long professorId) {
        try {
            UserDTO user = userServiceRestClient.get()
                    .uri("/api/users/{id}", professorId)
                    .retrieve()
                    .body(UserDTO.class);

            return user != null && "PROFESSOR".equals(user.role());
        } catch (HttpClientErrorException.NotFound e) {
            return false;
        }
    }
}
