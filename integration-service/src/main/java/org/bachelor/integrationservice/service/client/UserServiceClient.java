package org.bachelor.integrationservice.service.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bachelor.integrationservice.model.client.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserServiceClient {

    private final RestClient restClient;

    @Value("${user-service.url}")
    private String userServiceUrl;

    public List<UserDTO> getAllStudents(String authHeader) {
        try {
            List<UserDTO> users = restClient.get()
                    .uri(userServiceUrl + "/api/users")
                    .header("Authorization", authHeader)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});
            if (users == null) return List.of();
            return users.stream().filter(u -> "STUDENT".equals(u.getRole())).toList();
        } catch (Exception e) {
            log.error("Failed to get users: {}", e.getMessage());
            return List.of();
        }
    }

    public GroupDTO findGroup(String name, String authHeader) {
        try {
            PageResponse<GroupDTO> page = restClient.get()
                    .uri(userServiceUrl + "/api/groups?name={name}&size=1", name)
                    .header("Authorization", authHeader)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});
            return (page != null && page.getContent() != null && !page.getContent().isEmpty())
                    ? page.getContent().get(0) : null;
        } catch (Exception e) {
            log.error("Failed to find group {}: {}", name, e.getMessage());
            return null;
        }
    }

    public List<GroupMemberDTO> getGroupMembers(Long groupId, String authHeader) {
        try {
            List<GroupMemberDTO> members = restClient.get()
                    .uri(userServiceUrl + "/api/groups/{id}/members", groupId)
                    .header("Authorization", authHeader)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});
            return members != null ? members : List.of();
        } catch (Exception e) {
            log.error("Failed to get members for group {}: {}", groupId, e.getMessage());
            return List.of();
        }
    }

    public GroupMemberDTO addMemberToGroup(Long groupId, Long bookNumberId, String authHeader) {
        return restClient.post()
                .uri(userServiceUrl + "/api/groups/{groupId}/members/{bookNumberId}", groupId, bookNumberId)
                .header("Authorization", authHeader)
                .retrieve()
                .body(GroupMemberDTO.class);
    }

    public Long resolveSpecialtyOfferingId(Long bookNumberId, String authHeader) {
        try {
            BookNumberDTO book = restClient.get()
                    .uri(userServiceUrl + "/api/books/{id}", bookNumberId)
                    .header("Authorization", authHeader)
                    .retrieve()
                    .body(BookNumberDTO.class);
            return book != null ? book.getSpecialtyOfferingId() : null;
        } catch (Exception e) {
            log.error("Failed to get book number {}: {}", bookNumberId, e.getMessage());
            return null;
        }
    }

    public List<BookNumberDTO> getStudentBooks(Long studentId, String authHeader) {
        try {
            List<BookNumberDTO> books = restClient.get()
                    .uri(userServiceUrl + "/api/books/student/{studentId}", studentId)
                    .header("Authorization", authHeader)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});
            return books != null ? books : List.of();
        } catch (Exception e) {
            log.error("Failed to get books for student {}: {}", studentId, e.getMessage());
            return List.of();
        }
    }

    public BookNumberDTO findOrCreateBook(Long userId, Long offeringId, String authHeader) {
        try {
            List<BookNumberDTO> books = getStudentBooks(userId, authHeader);
            if (books != null && !books.isEmpty()) {
                return books.stream()
                        .filter(b -> b.getSpecialtyOfferingId() != null && b.getSpecialtyOfferingId().equals(offeringId))
                        .findFirst()
                        .orElseGet(() -> books.stream()
                                .filter(b -> "REGISTERED".equals(b.getStatus()) || "FILLED".equals(b.getStatus()))
                                .findFirst().orElse(null));
            }
            Map<String, Object> body = Map.of("studentId", userId, "specialtyOfferingId", offeringId);
            return restClient.post()
                    .uri(userServiceUrl + "/api/books")
                    .header("Authorization", authHeader)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(BookNumberDTO.class);
        } catch (Exception e) {
            log.error("Failed to find or create book for user {}: {}", userId, e.getMessage());
            return null;
        }
    }

    public UserDTO createStudent(Map<String, Object> studentData, String authHeader) {
        try {
            return restClient.post()
                    .uri(userServiceUrl + "/api/users/import-student")
                    .header("Authorization", authHeader)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(studentData)
                    .retrieve()
                    .body(UserDTO.class);
        } catch (Exception e) {
            log.error("Failed to create student: {}", e.getMessage());
            return null;
        }
    }
}
