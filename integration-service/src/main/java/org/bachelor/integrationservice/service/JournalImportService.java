package org.bachelor.integrationservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bachelor.integrationservice.model.client.*;
import org.bachelor.integrationservice.model.dto.ImportErrorDTO;
import org.bachelor.integrationservice.model.dto.JournalDisciplineStatusDTO;
import org.bachelor.integrationservice.model.dto.JournalImportRequestDTO;
import org.bachelor.integrationservice.model.dto.JournalImportResultDTO;
import org.bachelor.integrationservice.model.dto.JournalStudentStatusDTO;
import org.bachelor.integrationservice.model.journal.JournalDisciplineDTO;
import org.bachelor.integrationservice.model.journal.JournalDisciplineDetailDTO;
import org.bachelor.integrationservice.model.journal.JournalStudentDTO;
import org.bachelor.integrationservice.model.journal.JournalStudentGradeDTO;
import org.bachelor.integrationservice.service.journal.JournalClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class JournalImportService {

    private final JournalClient journalClient;
    private final org.springframework.web.client.RestClient restClient;

    @Value("${user-service.url}")
    private String userServiceUrl;

    @Value("${grade-service.url}")
    private String gradeServiceUrl;

    public List<JournalStudentStatusDTO> getStudentsWithStatus(long specialtyId, String authHeader) {
        List<JournalStudentDTO> journalStudents = journalClient.getStudents(specialtyId);
        Map<String, UserDTO> systemUserByEmail = getAllStudents(authHeader).stream()
                .filter(u -> u.getEmail() != null)
                .collect(Collectors.toMap(u -> u.getEmail().toLowerCase(), u -> u, (a, b) -> a));

        return journalStudents.stream().map(s -> {
            JournalStudentStatusDTO dto = new JournalStudentStatusDTO();
            dto.setExternalId(s.getExternalId());
            dto.setFirstName(s.getFirstName());
            dto.setLastName(s.getLastName());
            dto.setPatronymic(s.getPatronymic());
            dto.setEmail(s.getEmail());
            dto.setExistsInSystem(s.getEmail() != null
                    && systemUserByEmail.containsKey(s.getEmail().toLowerCase()));
            return dto;
        }).collect(Collectors.toList());
    }

    public List<JournalDisciplineStatusDTO> getDisciplinesWithStatus(long specialtyId, String authHeader) {
        List<JournalDisciplineDTO> journalDiscs = journalClient.getDisciplines(specialtyId);
        Map<String, DisciplineDTO> disciplineByName = getAllDisciplines(authHeader).stream()
                .collect(Collectors.toMap(d -> normKey(d.getName()), d -> d, (a, b) -> a));

        return journalDiscs.stream().map(d -> {
            JournalDisciplineStatusDTO dto = new JournalDisciplineStatusDTO();
            dto.setExternalId(d.getExternalId());
            dto.setName(d.getName());
            dto.setExistsInSystem(disciplineByName.containsKey(normKey(d.getName())));
            try {
                JournalDisciplineDetailDTO detail = journalClient.getDisciplineDetail(d.getExternalId());
                dto.setSemester(detail.getSemester());
                dto.setTotalHours(detail.getTotalHours());
                dto.setAcademicYear(detail.getAcademicYear());
                // collect unique attempt numbers from grades
                List<Integer> attempts = detail.getGrades().stream()
                        .map(JournalStudentGradeDTO::getAttempt)
                        .filter(java.util.Objects::nonNull)
                        .distinct().sorted().collect(Collectors.toList());
                dto.setAttempts(attempts.isEmpty() ? List.of(1) : attempts);
            } catch (Exception e) {
                log.warn("Could not fetch detail for discipline {}: {}", d.getExternalId(), e.getMessage());
                dto.setAttempts(List.of(1));
            }
            return dto;
        }).collect(Collectors.toList());
    }

    public JournalImportResultDTO importFromJournal(JournalImportRequestDTO request, String authHeader) {
        long specialtyId = request.getJournalSpecialtyId();
        long offeringId = request.getSpecialtyOfferingId();
        String academicYear = nonBlank(request.getAcademicYear(), defaultAcademicYear());
        Map<Long, Map<Integer, Long>> professorMap = request.getProfessorByDisciplineId();

        Set<Long> selectedStudentIds = (request.getSelectedStudentExternalIds() != null
                && !request.getSelectedStudentExternalIds().isEmpty())
                ? new HashSet<>(request.getSelectedStudentExternalIds())
                : null;

        // Build journal student index by externalId
        List<JournalStudentDTO> journalStudents = journalClient.getStudents(specialtyId);
        Map<Long, JournalStudentDTO> journalStudentById = journalStudents.stream()
                .collect(Collectors.toMap(JournalStudentDTO::getExternalId, s -> s));

        // Build discipline name index from list (detail no longer carries names)
        Map<Long, String> disciplineNameById = journalClient.getDisciplines(specialtyId).stream()
                .collect(Collectors.toMap(JournalDisciplineDTO::getExternalId, JournalDisciplineDTO::getName, (a, b) -> a));

        // Build system user index by email
        Map<String, UserDTO> systemUserByEmail = getAllStudents(authHeader).stream()
                .filter(u -> u.getEmail() != null)
                .collect(Collectors.toMap(u -> u.getEmail().toLowerCase(), u -> u, (a, b) -> a));

        // Pre-fetch all disciplines for name-based matching
        Map<String, DisciplineDTO> disciplineByName = new HashMap<>();
        for (DisciplineDTO d : getAllDisciplines(authHeader)) {
            disciplineByName.put(normKey(d.getName()), d);
        }

        List<String> unmatchedStudents = new ArrayList<>();
        List<ImportErrorDTO> errors = new ArrayList<>();
        Set<Long> matchedUserIds = new HashSet<>();
        int disciplinesProcessed = 0;
        int studentsMatched = 0;
        int gradesCreated = 0;

        for (Map.Entry<Long, Map<Integer, Long>> entry : professorMap.entrySet()) {
            long extDisciplineId = entry.getKey();
            Map<Integer, Long> attemptProfessorMap = entry.getValue();

            JournalDisciplineDetailDTO detail;
            try {
                detail = journalClient.getDisciplineDetail(extDisciplineId);
            } catch (Exception e) {
                log.error("Failed to fetch discipline {}: {}", extDisciplineId, e.getMessage());
                errors.add(new ImportErrorDTO(null, "external#" + extDisciplineId, "Не вдалося завантажити дисципліну"));
                continue;
            }

            // Use name from discipline list since detail no longer carries it
            String disciplineName = disciplineNameById.getOrDefault(extDisciplineId, detail.getName());
            detail.setName(disciplineName);

            SpecialtyDisciplineDTO sd;
            try {
                sd = resolveSpecialtyDiscipline(detail, offeringId, academicYear, disciplineByName, authHeader);
            } catch (Exception e) {
                log.error("Failed to resolve specialty-discipline '{}': {}", disciplineName, e.getMessage());
                errors.add(new ImportErrorDTO(null, disciplineName, "Помилка дисципліни: " + e.getMessage()));
                continue;
            }

            disciplinesProcessed++;
            List<Long> entryIdsToClose = new ArrayList<>();

            for (JournalStudentGradeDTO grade : detail.getGrades()) {
                if (selectedStudentIds != null && !selectedStudentIds.contains(grade.getStudentExternalId())) {
                    continue;
                }

                // Pick professor for this attempt; fall back to attempt=1 professor
                int attempt = grade.getAttempt() != null ? grade.getAttempt() : 1;
                Long professorId = attemptProfessorMap.getOrDefault(attempt,
                        attemptProfessorMap.values().stream().findFirst().orElse(null));
                if (professorId == null) {
                    String name = "externalId=" + grade.getStudentExternalId();
                    errors.add(new ImportErrorDTO(name, disciplineName, "Не вказано викладача для спроби " + attempt));
                    continue;
                }
                JournalStudentDTO jStudent = journalStudentById.get(grade.getStudentExternalId());
                if (jStudent == null) {
                    log.warn("No journal student for externalId={}", grade.getStudentExternalId());
                    unmatchedStudents.add("externalId=" + grade.getStudentExternalId());
                    continue;
                }

                UserDTO user = systemUserByEmail.get(jStudent.getEmail().toLowerCase());
                if (user == null) {
                    user = createStudentFromJournal(jStudent, authHeader);
                    if (user == null) {
                        String name = jStudent.getLastName() + " " + jStudent.getFirstName();
                        errors.add(new ImportErrorDTO(name, detail.getName(),
                                "Не вдалося створити студента: " + jStudent.getEmail()));
                        if (!unmatchedStudents.contains(name)) unmatchedStudents.add(name);
                        continue;
                    }
                    systemUserByEmail.put(user.getEmail().toLowerCase(), user);
                }

                BookNumberDTO book = findOrCreateBook(user.getId(), offeringId, authHeader);
                if (book == null) {
                    String name = jStudent.getLastName() + " " + jStudent.getFirstName();
                    errors.add(new ImportErrorDTO(name, detail.getName(), "Не вдалося знайти або створити залікову книжку"));
                    continue;
                }

                if (matchedUserIds.add(user.getId())) studentsMatched++;

                try {
                    GradeBookEntryDTO gbe = findOrCreateEntry(
                            book.getId(), sd.getId(), professorId, academicYear, detail.getSemester(), attempt, authHeader);
                    createGrade(gbe.getId(), grade.getUniversityGrade(), grade.getAssessmentDate(), authHeader);
                    entryIdsToClose.add(gbe.getId());
                    gradesCreated++;
                } catch (Exception e) {
                    String name = jStudent.getLastName() + " " + jStudent.getFirstName();
                    log.error("Grade save failed for {} / {}: {}", name, detail.getName(), e.getMessage());
                    errors.add(new ImportErrorDTO(name, detail.getName(), e.getMessage()));
                }
            }

            if (!entryIdsToClose.isEmpty()) {
                try {
                    closeEntries(entryIdsToClose, authHeader);
                } catch (Exception e) {
                    log.warn("Failed to close entries: {}", e.getMessage());
                }
            }
        }

        return JournalImportResultDTO.builder()
                .disciplinesProcessed(disciplinesProcessed)
                .studentsMatched(studentsMatched)
                .studentsUnmatched(unmatchedStudents.size())
                .gradesCreated(gradesCreated)
                .unmatchedStudents(unmatchedStudents)
                .errors(errors)
                .build();
    }

    // ── Discipline resolution ────────────────────────────────────────────────

    private SpecialtyDisciplineDTO resolveSpecialtyDiscipline(
            JournalDisciplineDetailDTO detail, long offeringId, String academicYear,
            Map<String, DisciplineDTO> disciplineByName, String authHeader) {

        DisciplineDTO existing = disciplineByName.get(normKey(detail.getName()));
        if (existing != null) {
            SpecialtyDisciplineDTO sd = findSpecialtyDiscipline(offeringId, existing.getId(), authHeader);
            if (sd != null) return sd;
            return createSpecialtyDisciplineWithHours(offeringId, existing.getId(), academicYear, detail, authHeader);
        }

        DisciplineCreateResponseDTO created = createDisciplineWithSD(detail, offeringId, academicYear, authHeader);
        DisciplineDTO newDisc = new DisciplineDTO();
        newDisc.setId(created.getDisciplineId());
        newDisc.setName(detail.getName());
        disciplineByName.put(normKey(detail.getName()), newDisc);
        SpecialtyDisciplineDTO sd = new SpecialtyDisciplineDTO();
        sd.setId(created.getSpecialtyDisciplineId());
        return sd;
    }

    private DisciplineCreateResponseDTO createDisciplineWithSD(
            JournalDisciplineDetailDTO detail, long offeringId, String academicYear, String authHeader) {
        Map<String, Object> hours = buildHours(academicYear, detail);
        Map<String, Object> body = new HashMap<>();
        body.put("name", detail.getName());
        body.put("specialtyOfferingId", offeringId);
        body.put("hours", hours);
        return restClient.post()
                .uri(gradeServiceUrl + "/api/disciplines")
                .header("Authorization", authHeader)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(DisciplineCreateResponseDTO.class);
    }

    private SpecialtyDisciplineDTO createSpecialtyDisciplineWithHours(
            long offeringId, long disciplineId, String academicYear,
            JournalDisciplineDetailDTO detail, String authHeader) {
        SpecialtyDisciplineDTO sd = restClient.post()
                .uri(gradeServiceUrl + "/api/specialty-disciplines/{oid}?disciplineId={did}",
                        offeringId, disciplineId)
                .header("Authorization", authHeader)
                .retrieve()
                .body(SpecialtyDisciplineDTO.class);
        try {
            restClient.post()
                    .uri(gradeServiceUrl + "/api/hours?specialtyDisciplineId={id}", sd.getId())
                    .header("Authorization", authHeader)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(buildHours(academicYear, detail))
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception e) {
            log.warn("Could not save hours for sd {}: {}", sd.getId(), e.getMessage());
        }
        return sd;
    }

    private SpecialtyDisciplineDTO findSpecialtyDiscipline(long offeringId, long disciplineId, String authHeader) {
        try {
            List<SpecialtyDisciplineDTO> list = restClient.get()
                    .uri(gradeServiceUrl + "/api/specialty-disciplines?specialtyOfferingId={s}&disciplineId={d}",
                            offeringId, disciplineId)
                    .header("Authorization", authHeader)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});
            return (list != null && !list.isEmpty()) ? list.get(0) : null;
        } catch (Exception e) {
            return null;
        }
    }

    private Map<String, Object> buildHours(String academicYear, JournalDisciplineDetailDTO detail) {
        int ects = detail.getEctsCredits() != null ? detail.getEctsCredits() : 1;
        int total = detail.getTotalHours() != null ? detail.getTotalHours() : ects * 30;
        Map<String, Object> h = new HashMap<>();
        h.put("academicYear", academicYear);
        h.put("ectsCredits", ects);
        h.put("totalHours", total);
        h.put("classroomHours", 0);
        return h;
    }

    // ── Grade book entry ─────────────────────────────────────────────────────

    private GradeBookEntryDTO findOrCreateEntry(long bookId, long sdId, long professorId,
                                                 String academicYear, Integer semester, int attempt, String authHeader) {
        PageResponse<GradeBookEntryDTO> page = restClient.get()
                .uri(gradeServiceUrl + "/api/records?bookNumberId={b}&specialtyDisciplineId={sd}&academicYear={y}&size=200",
                        bookId, sdId, academicYear)
                .header("Authorization", authHeader)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});

        List<GradeBookEntryDTO> existing = (page != null && page.getContent() != null) ? page.getContent() : List.of();
        if (!existing.isEmpty()) {
            GradeBookEntryDTO match = existing.stream()
                    .filter(e -> java.util.Objects.equals(e.getAttempt(), attempt))
                    .filter(e -> java.util.Objects.equals(e.getProfessorId(), professorId))
                    .filter(e -> semester == null || semester.equals(e.getSemester()))
                    .findFirst().orElse(null);
            if (match != null) return match;
        }

        Map<String, Object> body = new HashMap<>();
        body.put("specialtyDisciplineId", sdId);
        body.put("professorId", professorId);
        body.put("academicYear", academicYear);
        body.put("bookNumberIds", List.of(bookId));
        body.put("minAttempt", attempt);
        if (semester != null) body.put("semester", semester);

        List<GradeBookEntryDTO> created = restClient.post()
                .uri(gradeServiceUrl + "/api/records")
                .header("Authorization", authHeader)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});

        if (created == null || created.isEmpty())
            throw new IllegalStateException("Failed to create grade book entry");
        return created.get(0);
    }

    private void createGrade(long entryId, Integer universityGrade, String assessmentDate, String authHeader) {
        Map<String, Object> body = new HashMap<>();
        body.put("entryId", entryId);
        body.put("assessmentType", "EXAM");
        body.put("assessmentDate", assessmentDate != null ? assessmentDate + "T00:00:00" : LocalDate.now() + "T00:00:00");
        if (universityGrade != null) body.put("universityGrade", universityGrade);
        restClient.post()
                .uri(gradeServiceUrl + "/api/grades")
                .header("Authorization", authHeader)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .toBodilessEntity();
    }

    private void closeEntries(List<Long> entryIds, String authHeader) {
        restClient.patch()
                .uri(gradeServiceUrl + "/api/records/close")
                .header("Authorization", authHeader)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("entryIds", entryIds))
                .retrieve()
                .toBodilessEntity();
    }

    // ── User / book helpers ──────────────────────────────────────────────────

    private List<UserDTO> getAllStudents(String authHeader) {
        try {
            List<UserDTO> users = restClient.get()
                    .uri(userServiceUrl + "/api/users")
                    .header("Authorization", authHeader)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});
            return users == null ? List.of()
                    : users.stream().filter(u -> "STUDENT".equals(u.getRole())).toList();
        } catch (Exception e) {
            log.error("Failed to fetch students: {}", e.getMessage());
            return List.of();
        }
    }

    private List<DisciplineDTO> getAllDisciplines(String authHeader) {
        try {
            List<DisciplineDTO> list = restClient.get()
                    .uri(gradeServiceUrl + "/api/disciplines")
                    .header("Authorization", authHeader)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});
            return list != null ? list : List.of();
        } catch (Exception e) {
            log.error("Failed to fetch disciplines: {}", e.getMessage());
            return List.of();
        }
    }

    private UserDTO createStudentFromJournal(JournalStudentDTO student, String authHeader) {
        try {
            Map<String, Object> body = new HashMap<>();
            body.put("firstName", student.getFirstName());
            body.put("lastName", student.getLastName());
            body.put("patronymic", student.getPatronymic());
            body.put("email", student.getEmail());
            return restClient.post()
                    .uri(userServiceUrl + "/api/users/import-student")
                    .header("Authorization", authHeader)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(UserDTO.class);
        } catch (Exception e) {
            log.error("Failed to create student {} from journal: {}", student.getEmail(), e.getMessage());
            return null;
        }
    }

    /** Returns a book for the student: prefers matching offering, falls back to any active book, creates if none exist. */
    private BookNumberDTO findOrCreateBook(long userId, long offeringId, String authHeader) {
        try {
            List<BookNumberDTO> books = restClient.get()
                    .uri(userServiceUrl + "/api/books/student/{id}", userId)
                    .header("Authorization", authHeader)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});

            if (books != null && !books.isEmpty()) {
                return books.stream()
                        .filter(b -> b.getSpecialtyOfferingId() != null && b.getSpecialtyOfferingId().equals(offeringId))
                        .findFirst()
                        .orElseGet(() -> books.stream()
                                .filter(b -> "REGISTERED".equals(b.getStatus()) || "FILLED".equals(b.getStatus()))
                                .findFirst().orElse(null));
            }

            // No books at all — create one without a number
            Map<String, Object> body = new HashMap<>();
            body.put("studentId", userId);
            body.put("specialtyOfferingId", offeringId);
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

    // ── Utility ──────────────────────────────────────────────────────────────

    private static String normKey(String name) {
        if (name == null) return null;
        return name.trim().toLowerCase()
                .replace('а', 'a').replace('е', 'e').replace('о', 'o')
                .replace('с', 'c').replace('р', 'p').replace('х', 'x')
                .replace('і', 'i');
    }

    private static String nonBlank(String s, String fallback) {
        return (s != null && !s.isBlank()) ? s : fallback;
    }

    private static String defaultAcademicYear() {
        int y = LocalDate.now().getYear();
        return y + "/" + (y + 1);
    }
}
