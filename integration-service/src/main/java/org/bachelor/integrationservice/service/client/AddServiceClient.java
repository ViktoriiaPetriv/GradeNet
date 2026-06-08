package org.bachelor.integrationservice.service.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class AddServiceClient {

    private final RestClient restClient;

    @Value("${add-service.url}")
    private String addServiceUrl;

    public void createAdditionalWork(long bookNumberId, long commissionId, String type,
                                     String title, String eventDate,
                                     Integer universityGrade, String ectsGrade, String nationalGrade,
                                     String authHeader) {
        Map<String, Object> body = new HashMap<>();
        body.put("bookNumberId", bookNumberId);
        body.put("commissionId", commissionId);
        body.put("type", type);
        body.put("title", title);
        if (eventDate != null) body.put("eventDate", eventDate);
        if (universityGrade != null) body.put("universityGrade", universityGrade);
        if (ectsGrade != null) body.put("ectsGrade", ectsGrade);
        if (nationalGrade != null) body.put("nationalGrade", nationalGrade);

        restClient.post()
                .uri(addServiceUrl + "/api/works")
                .header("Authorization", authHeader)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .toBodilessEntity();
    }
}
