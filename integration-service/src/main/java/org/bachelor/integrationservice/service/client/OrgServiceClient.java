package org.bachelor.integrationservice.service.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bachelor.integrationservice.model.client.PageResponse;
import org.bachelor.integrationservice.model.client.SpecialtyDTO;
import org.bachelor.integrationservice.service.ExcelParserService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrgServiceClient {

    private final RestClient restClient;

    @Value("${org-service.url}")
    private String orgServiceUrl;

    public SpecialtyDTO getSpecialtyById(Long specialtyId, String authHeader) {
        try {
            return restClient.get()
                    .uri(orgServiceUrl + "/api/specialties/{id}", specialtyId)
                    .header("Authorization", authHeader)
                    .retrieve()
                    .body(SpecialtyDTO.class);
        } catch (Exception e) {
            log.error("Failed to fetch specialty {}: {}", specialtyId, e.getMessage());
            return null;
        }
    }

    public SpecialtyDTO resolveSpecialtyByName(String specialtyName, String authHeader) {
        if (specialtyName == null || specialtyName.isBlank()) return null;
        try {
            String normalized = normalizeForMatch(specialtyName);
            PageResponse<SpecialtyDTO> page = restClient.get()
                    .uri(orgServiceUrl + "/api/specialties?size=200")
                    .header("Authorization", authHeader)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});
            if (page == null || page.getContent() == null) return null;
            return page.getContent().stream()
                    .filter(s -> matches(s.getNameUA(), normalized)
                            || matches(s.getStudyProgramUA(), normalized)
                            || matches(s.getEduProgramUA(), normalized))
                    .findFirst().orElse(null);
        } catch (Exception e) {
            log.error("Failed to resolve specialty by name '{}': {}", specialtyName, e.getMessage());
            return null;
        }
    }

    public Long resolveOfferingIdBySpecialtyId(Long specialtyId, Integer graduationYear, String authHeader) {
        try {
            record OfferingInfo(Long id, Integer graduationYear) {}
            List<OfferingInfo> offerings = restClient.get()
                    .uri(orgServiceUrl + "/api/specialty-offerings?specialtyId={id}", specialtyId)
                    .header("Authorization", authHeader)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});
            if (offerings == null || offerings.isEmpty()) return null;
            if (graduationYear != null) {
                Optional<OfferingInfo> exact = offerings.stream()
                        .filter(o -> graduationYear.equals(o.graduationYear()))
                        .findFirst();
                if (exact.isPresent()) {
                    log.info("Matched specialty offering by graduation year {}: id={}", graduationYear, exact.get().id());
                    return exact.get().id();
                }
                log.warn("No offering found for graduation year {}", graduationYear);
            }
            return null;
        } catch (Exception e) {
            log.error("Failed to fetch offerings for specialty {}: {}", specialtyId, e.getMessage());
            return null;
        }
    }

    public Integer getOfferingGraduationYear(Long offeringId, String authHeader) {
        if (offeringId == null) return null;
        try {
            record OfferingInfo(Long id, Integer graduationYear) {}
            OfferingInfo info = restClient.get()
                    .uri(orgServiceUrl + "/api/specialty-offerings/{id}", offeringId)
                    .header("Authorization", authHeader)
                    .retrieve()
                    .body(OfferingInfo.class);
            return info != null ? info.graduationYear() : null;
        } catch (Exception e) {
            log.warn("Failed to fetch graduation year for offering {}: {}", offeringId, e.getMessage());
            return null;
        }
    }

    public Long resolveOfferingBySpecialtyName(String specialtyName, String academicYear,
                                                Integer semester, String authHeader) {
        if (specialtyName == null || specialtyName.isBlank()) return null;
        try {
            SpecialtyDTO specialty = resolveSpecialtyByName(specialtyName, authHeader);
            if (specialty == null) return null;
            Integer graduationYear = ExcelParserService.calculateGraduationYear(
                    academicYear, semester, specialty.getDegree());
            return resolveOfferingIdBySpecialtyId(specialty.getId(), graduationYear, authHeader);
        } catch (Exception e) {
            log.error("Failed to resolve specialty offering by name '{}': {}", specialtyName, e.getMessage());
            return null;
        }
    }

    private static String normalizeForMatch(String s) {
        if (s == null) return null;
        return s.trim().toLowerCase()
                .replace('‘', '\'').replace('ʼ', '\'').replace('’', '\'');
    }

    private static boolean matches(String field, String normalizedQuery) {
        return field != null && normalizeForMatch(field).equals(normalizedQuery);
    }
}
