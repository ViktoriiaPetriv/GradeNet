package org.bachelor.integrationservice.service.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bachelor.integrationservice.model.ParsedDiscipline;
import org.bachelor.integrationservice.model.ParsedGrade;
import org.bachelor.integrationservice.model.client.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class GradeServiceClient {

    private final RestClient restClient;

    @Value("${grade-service.url}")
    private String gradeServiceUrl;

    public List<DisciplineDTO> getAllDisciplines(String authHeader) {
        try {
            List<DisciplineDTO> list = restClient.get()
                    .uri(gradeServiceUrl + "/api/disciplines")
                    .header("Authorization", authHeader)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});
            return list != null ? list : List.of();
        } catch (Exception e) {
            log.error("Failed to get disciplines: {}", e.getMessage());
            return List.of();
        }
    }

    public DisciplineCreateResponseDTO createDisciplineWithSD(
            String name, Long specialtyOfferingId, String academicYear,
            ParsedDiscipline parsed) {
        Map<String, Object> hours = buildHoursBody(academicYear, parsed);
        Map<String, Object> body = new HashMap<>();
        body.put("name", name);
        body.put("specialtyOfferingId", specialtyOfferingId);
        body.put("hours", hours);
        return restClient.post()
                .uri(gradeServiceUrl + "/api/disciplines")
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(DisciplineCreateResponseDTO.class);
    }

    public DisciplineCreateResponseDTO createDisciplineWithSD(
            String name, Long specialtyOfferingId, String academicYear,
            ParsedDiscipline parsed, String authHeader) {
        Map<String, Object> hours = buildHoursBody(academicYear, parsed);
        Map<String, Object> body = new HashMap<>();
        body.put("name", name);
        body.put("specialtyOfferingId", specialtyOfferingId);
        body.put("hours", hours);
        return restClient.post()
                .uri(gradeServiceUrl + "/api/disciplines")
                .header("Authorization", authHeader)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(DisciplineCreateResponseDTO.class);
    }

    public SpecialtyDisciplineDTO findSpecialtyDiscipline(Long specialtyOfferingId, Long disciplineId, String authHeader) {
        try {
            List<SpecialtyDisciplineDTO> list = restClient.get()
                    .uri(gradeServiceUrl + "/api/specialty-disciplines?specialtyOfferingId={s}&disciplineId={d}",
                            specialtyOfferingId, disciplineId)
                    .header("Authorization", authHeader)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});
            return (list != null && !list.isEmpty()) ? list.get(0) : null;
        } catch (Exception e) {
            return null;
        }
    }

    public SpecialtyDisciplineDTO createSpecialtyDiscipline(Long specialtyOfferingId, Long disciplineId, String authHeader) {
        return restClient.post()
                .uri(gradeServiceUrl + "/api/specialty-disciplines/{specialtyOfferingId}?disciplineId={disciplineId}",
                        specialtyOfferingId, disciplineId)
                .header("Authorization", authHeader)
                .retrieve()
                .body(SpecialtyDisciplineDTO.class);
    }

    public SpecialtyDisciplineDTO createSpecialtyDisciplineWithHours(
            Long specialtyOfferingId, Long disciplineId, String academicYear,
            ParsedDiscipline parsed, String authHeader) {
        SpecialtyDisciplineDTO sd = createSpecialtyDiscipline(specialtyOfferingId, disciplineId, authHeader);
        try {
            restClient.post()
                    .uri(gradeServiceUrl + "/api/hours?specialtyDisciplineId={id}", sd.getId())
                    .header("Authorization", authHeader)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(buildHoursBody(academicYear, parsed))
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception e) {
            log.warn("Could not save hours for specialty-discipline {}: {}", sd.getId(), e.getMessage());
        }
        return sd;
    }

    public GradeBookEntryDTO findOrCreateEntry(Long bookNumberId, Long sdId, Long professorId,
                                               String academicYear, Integer semester,
                                               boolean overwrite, String authHeader) {
        PageResponse<GradeBookEntryDTO> page = restClient.get()
                .uri(gradeServiceUrl + "/api/records?bookNumberId={b}&specialtyDisciplineId={sd}&academicYear={y}&size=200",
                        bookNumberId, sdId, academicYear)
                .header("Authorization", authHeader)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
        List<GradeBookEntryDTO> existing = (page != null && page.getContent() != null) ? page.getContent() : List.of();

        if (!existing.isEmpty()) {
            if (semester != null) {
                GradeBookEntryDTO match = existing.stream()
                        .filter(e -> semester.equals(e.getSemester()))
                        .findFirst().orElse(null);
                if (match != null) {
                    if (overwrite && "COMPLETED".equals(match.getStatus())) resetEntry(match.getId(), authHeader);
                    return match;
                }
            } else {
                GradeBookEntryDTO match = existing.get(0);
                if (overwrite && "COMPLETED".equals(match.getStatus())) resetEntry(match.getId(), authHeader);
                return match;
            }
        }

        Map<String, Object> body = new HashMap<>();
        body.put("specialtyDisciplineId", sdId);
        body.put("professorId", professorId);
        body.put("academicYear", academicYear);
        body.put("bookNumberIds", List.of(bookNumberId));
        if (semester != null) body.put("semester", semester);

        List<GradeBookEntryDTO> created = restClient.post()
                .uri(gradeServiceUrl + "/api/records")
                .header("Authorization", authHeader)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});

        if (created == null || created.isEmpty()) throw new IllegalStateException("Failed to create grade book entry");
        return created.get(0);
    }

    public GradeBookEntryDTO findOrCreateRetakeEntry(Long bookNumberId, Long sdId,
                                                      Long professorId, String academicYear,
                                                      Integer semester, String authHeader) {
        PageResponse<GradeBookEntryDTO> page = restClient.get()
                .uri(gradeServiceUrl + "/api/records?bookNumberId={b}&specialtyDisciplineId={sd}&size=200",
                        bookNumberId, sdId)
                .header("Authorization", authHeader)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
        List<GradeBookEntryDTO> all = (page != null && page.getContent() != null) ? page.getContent() : List.of();

        if (!all.isEmpty()) {
            GradeBookEntryDTO prev = all.stream()
                    .max(Comparator.comparingInt(e -> e.getAttempt() != null ? e.getAttempt() : 0))
                    .get();

            if ("IN_PROGRESS".equals(prev.getStatus())) {
                int targetAttempt = Math.max(2, prev.getAttempt() != null ? prev.getAttempt() : 1);
                String year = prev.getAcademicYear() != null ? prev.getAcademicYear() : academicYear;
                restClient.delete()
                        .uri(gradeServiceUrl + "/api/records/{id}", prev.getId())
                        .header("Authorization", authHeader)
                        .retrieve()
                        .toBodilessEntity();
                log.info("Deleted empty IN_PROGRESS entry {} for retake; will create attempt {}", prev.getId(), targetAttempt);
                return createEntryWithMinAttempt(bookNumberId, sdId, professorId, year, targetAttempt, semester, authHeader);
            }

            return restClient.post()
                    .uri(gradeServiceUrl + "/api/records/{id}/retake?professorId={p}", prev.getId(), professorId)
                    .header("Authorization", authHeader)
                    .retrieve()
                    .body(GradeBookEntryDTO.class);
        }

        log.warn("Retake discipline has no prior entry for bookNumberId={}, sdId={} — creating as attempt 2", bookNumberId, sdId);
        return createEntryWithMinAttempt(bookNumberId, sdId, professorId, academicYear, 2, semester, authHeader);
    }

    public GradeBookEntryDTO createEntryWithMinAttempt(Long bookNumberId, Long sdId, Long professorId,
                                                        String academicYear, int minAttempt,
                                                        Integer semester, String authHeader) {
        Map<String, Object> body = new HashMap<>();
        body.put("specialtyDisciplineId", sdId);
        body.put("professorId", professorId);
        body.put("academicYear", academicYear);
        body.put("bookNumberIds", List.of(bookNumberId));
        body.put("minAttempt", minAttempt);
        if (semester != null) body.put("semester", semester);
        List<GradeBookEntryDTO> created = restClient.post()
                .uri(gradeServiceUrl + "/api/records")
                .header("Authorization", authHeader)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
        if (created == null || created.isEmpty()) throw new IllegalStateException("Failed to create grade book entry");
        return created.get(0);
    }

    public void createGrade(Long entryId, ParsedGrade grade, String authHeader) {
        String assessmentType = grade.getNationalGrade() instanceof String s
                && ("Зар.".equals(s) || "Нзр.".equals(s)) ? "CREDIT" : "EXAM";
        Map<String, Object> body = new HashMap<>();
        body.put("entryId", entryId);
        body.put("assessmentDate", LocalDateTime.now().toString());
        body.put("assessmentType", assessmentType);
        if (grade.getUniversityGrade() != null) body.put("universityGrade", grade.getUniversityGrade());
        restClient.post()
                .uri(gradeServiceUrl + "/api/grades")
                .header("Authorization", authHeader)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .toBodilessEntity();
    }

    public void createGrade(Long entryId, Integer universityGrade, String assessmentDate, Integer assessmentTypeCode, String authHeader) {
        Map<String, Object> body = new HashMap<>();
        body.put("entryId", entryId);
        body.put("assessmentType", convertAssessmentType(assessmentTypeCode));
        body.put("assessmentDate", assessmentDate != null ? assessmentDate + "T00:00:00" : LocalDateTime.now().toString());
        if (universityGrade != null) body.put("universityGrade", universityGrade);
        restClient.post()
                .uri(gradeServiceUrl + "/api/grades")
                .header("Authorization", authHeader)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .toBodilessEntity();
    }

    private String convertAssessmentType(Integer code) {
        return switch (code) {
            case 12  -> "CREDIT"; // залік
            case 11,
                 31  -> "EXAM";   // екзамен, державний іспит
            default  -> throw new IllegalArgumentException("Невідомий тип контролю: " + code);
        };
    }

    public void closeEntries(List<Long> entryIds, String authHeader) {
        restClient.patch()
                .uri(gradeServiceUrl + "/api/records/close")
                .header("Authorization", authHeader)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("entryIds", entryIds))
                .retrieve()
                .toBodilessEntity();
    }

    public void resetEntry(Long entryId, String authHeader) {
        try {
            restClient.patch()
                    .uri(gradeServiceUrl + "/api/records/{id}/reset", entryId)
                    .header("Authorization", authHeader)
                    .retrieve()
                    .toBodilessEntity();
            log.info("Reset entry {} for overwrite", entryId);
        } catch (Exception e) {
            log.error("Failed to reset entry {}: {}", entryId, e.getMessage());
            throw new RuntimeException("Не вдалося скинути запис " + entryId, e);
        }
    }

    public List<BulkEntryClientDTO> getGroupReportForDiscipline(Long sdId, String academicYear, String authHeader) {
        try {
            List<BulkEntryClientDTO> entries = restClient.get()
                    .uri(gradeServiceUrl + "/api/records/group-report?specialtyDisciplineId={sd}&academicYear={y}",
                            sdId, academicYear)
                    .header("Authorization", authHeader)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});
            return entries != null ? entries : List.of();
        } catch (Exception e) {
            log.warn("Failed to fetch group report for sd={}: {}", sdId, e.getMessage());
            return List.of();
        }
    }

    public DisciplineCreateResponseDTO createDisciplineWithSD(
            String name, Long specialtyOfferingId, Map<String, Object> hours, String authHeader) {
        Map<String, Object> body = new HashMap<>();
        body.put("name", name);
        body.put("specialtyOfferingId", specialtyOfferingId);
        body.put("hours", hours);
        return restClient.post()
                .uri(gradeServiceUrl + "/api/disciplines")
                .header("Authorization", authHeader)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(DisciplineCreateResponseDTO.class);
    }

    public SpecialtyDisciplineDTO createSpecialtyDisciplineWithHours(
            Long specialtyOfferingId, Long disciplineId, Map<String, Object> hours, String authHeader) {
        SpecialtyDisciplineDTO sd = createSpecialtyDiscipline(specialtyOfferingId, disciplineId, authHeader);
        try {
            restClient.post()
                    .uri(gradeServiceUrl + "/api/hours?specialtyDisciplineId={id}", sd.getId())
                    .header("Authorization", authHeader)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(hours)
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception e) {
            log.warn("Could not save hours for specialty-discipline {}: {}", sd.getId(), e.getMessage());
        }
        return sd;
    }

    public GradeBookEntryDTO findOrCreateEntryWithAttempt(Long bookId, Long sdId, Long professorId,
                                                           String academicYear, Integer semester,
                                                           int attempt, String authHeader) {
        PageResponse<GradeBookEntryDTO> page = restClient.get()
                .uri(gradeServiceUrl + "/api/records?bookNumberId={b}&specialtyDisciplineId={sd}&academicYear={y}&size=200",
                        bookId, sdId, academicYear)
                .header("Authorization", authHeader)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
        List<GradeBookEntryDTO> existing = (page != null && page.getContent() != null) ? page.getContent() : List.of();
        if (!existing.isEmpty()) {
            GradeBookEntryDTO match = existing.stream()
                    .filter(e -> Objects.equals(e.getAttempt(), attempt))
                    .filter(e -> Objects.equals(e.getProfessorId(), professorId))
                    .filter(e -> semester == null || semester.equals(e.getSemester()))
                    .findFirst().orElse(null);
            if (match != null) return match;
        }
        return createEntryWithMinAttempt(bookId, sdId, professorId, academicYear, attempt, semester, authHeader);
    }

    private Map<String, Object> buildHoursBody(String academicYear, ParsedDiscipline parsed) {
        Map<String, Object> hours = new HashMap<>();
        hours.put("academicYear", academicYear);
        hours.put("totalHours", parsed.getTotalHours());
        hours.put("ectsCredits", parsed.getEctsCredits());
        hours.put("classroomHours", 0);
        return hours;
    }
}
