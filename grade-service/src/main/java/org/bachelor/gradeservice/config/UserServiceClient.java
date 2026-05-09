package org.bachelor.gradeservice.config;

import lombok.RequiredArgsConstructor;
import org.bachelor.gradeservice.model.dto.BookNumberResponse;
import org.bachelor.gradeservice.model.dto.UserResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
@RequiredArgsConstructor
public class UserServiceClient {

    private final RestClient userServiceRestClient;

    public boolean professorExists(Long professorId) {
        try {
            UserResponse user = userServiceRestClient.get()
                    .uri("/api/users/{id}", professorId)
                    .retrieve()
                    .body(UserResponse.class);

            return user != null && "PROFESSOR".equals(user.role());
        } catch (HttpClientErrorException.NotFound e) {
            return false;
        }
    }

    public String getStudentName(Long bookNumberId) {
        try {
            BookNumberResponse book = userServiceRestClient.get()
                    .uri("/api/books/{id}", bookNumberId)
                    .retrieve()
                    .body(BookNumberResponse.class);

            if (book == null || book.studentId() == null) return fallbackName(bookNumberId);

            UserResponse user = userServiceRestClient.get()
                    .uri("/api/users/{id}", book.studentId())
                    .retrieve()
                    .body(UserResponse.class);

            if (user == null) return fallbackName(bookNumberId);

            String name = Stream.of(user.lastName(), user.firstName(), user.patronymic())
                    .filter(s -> s != null && !s.isBlank())
                    .collect(Collectors.joining(" "));

            return name.isBlank() ? fallbackName(bookNumberId) : name;
        } catch (Exception e) {
            return fallbackName(bookNumberId);
        }
    }

    private String fallbackName(Long bookNumberId) {
        return "Студент #" + bookNumberId;
    }
}
