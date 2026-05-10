package org.bachelor.integrationservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bachelor.integrationservice.model.ParsedDiscipline;
import org.bachelor.integrationservice.model.ParsedGrade;
import org.bachelor.integrationservice.model.ParsedReport;
import org.bachelor.integrationservice.model.ParsedStudentRow;
import org.bachelor.integrationservice.model.client.*;
import org.bachelor.integrationservice.model.dto.ImportErrorDTO;
import org.bachelor.integrationservice.model.dto.ImportResultDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class ImportService {

    private final ExcelParserService excelParserService;
    private final org.springframework.web.client.RestClient restClient;

    @Value("${user-service.url}")
    private String userServiceUrl;

    @Value("${org-service.url}")
    private String orgServiceUrl;

    @Value("${grade-service.url}")
    private String gradeServiceUrl;

    /**
     * @param professorByDiscipline map of discipline index → professorId;
     *                              disciplines absent from the map are skipped
     */
    public ImportResultDTO importGradeReport(MultipartFile file,
                                             Map<Integer, Long> professorByDiscipline,
                                             String authHeader) throws IOException {

        ParsedReport report = excelParserService.parse(file.getInputStream());
        String academicYear = (report.getAcademicYear() != null && !report.getAcademicYear().isBlank())
                ? report.getAcademicYear() : defaultAcademicYear();

        List<ImportErrorDTO> errors = new ArrayList<>();
        List<String> unmatchedStudents = new ArrayList<>();

        GroupDTO group = findGroup(report.getGroupName(), authHeader);
        if (group == null) {
            errors.add(new ImportErrorDTO(null, null, "Групу не знайдено: " + report.getGroupName()));
            return buildResult(report, 0, 0, 0, unmatchedStudents, errors);
        }

        List<GroupMemberDTO> members = new ArrayList<>(getGroupMembers(group.getId(), authHeader));
        if (members.isEmpty()) {
            log.info("Group {} has no members, searching system students by name", group.getName());
            members = findAndAddGroupMembers(group.getId(), report.getStudents(), authHeader, errors);
            if (members.isEmpty()) {
                errors.add(new ImportErrorDTO(null, null,
                        "Не вдалося знайти студентів у системі для групи: " + group.getName()));
                return buildResult(report, 0, 0, 0, unmatchedStudents, errors);
            }
        }

        Long specialtyId = resolveSpecialtyIdByName(report.getSpecialtyName(), authHeader);
        if (specialtyId == null) {
            log.warn("Could not resolve specialty by name '{}', falling back to book number lookup",
                    report.getSpecialtyName());
            specialtyId = resolveSpecialtyId(members.get(0).getBookNumberId(), authHeader);
        }
        if (specialtyId == null) {
            errors.add(new ImportErrorDTO(null, null, "Не вдалося визначити спеціальність з файлу або залікових книжок"));
            return buildResult(report, 0, 0, 0, unmatchedStudents, errors);
        }

        List<SpecialtyDisciplineDTO> specialtyDisciplines =
                resolveSpecialtyDisciplines(report.getDisciplines(), specialtyId,
                        academicYear, authHeader, errors);

        int studentsMatched = 0;
        int gradesCreated = 0;

        for (ParsedStudentRow studentRow : report.getStudents()) {
            GroupMemberDTO member = matchStudent(studentRow.getFullName(), members);
            if (member == null) {
                unmatchedStudents.add(studentRow.getFullName());
                log.warn("Could not match student: {}", studentRow.getFullName());
                continue;
            }
            studentsMatched++;

            List<Long> entryIdsToClose = new ArrayList<>();
            for (ParsedGrade grade : studentRow.getGrades()) {
                int idx = grade.getDisciplineIndex();
                Long professorId = professorByDiscipline.get(idx);
                if (professorId == null) continue; // discipline skipped — no professor assigned

                if (idx >= specialtyDisciplines.size()) continue;
                SpecialtyDisciplineDTO sd = specialtyDisciplines.get(idx);
                if (sd == null) continue;

                String disciplineName = report.getDisciplineNames().get(idx);
                boolean isRetake = !stripRetakeSuffix(disciplineName).equals(disciplineName);
                try {
                    GradeBookEntryDTO entry = isRetake
                            ? findOrCreateRetakeEntry(member.getBookNumberId(), sd.getId(), professorId, academicYear, authHeader)
                            : findOrCreateEntry(member.getBookNumberId(), sd.getId(), professorId, academicYear, authHeader);
                    createGrade(entry.getId(), grade, authHeader);
                    entryIdsToClose.add(entry.getId());
                    gradesCreated++;
                } catch (Exception e) {
                    log.error("Failed to create grade for {} / {}: {}",
                            studentRow.getFullName(), disciplineName, e.getMessage());
                    errors.add(new ImportErrorDTO(studentRow.getFullName(), disciplineName, e.getMessage()));
                }
            }

            // Close all entries only after all grades for this student are processed.
            // Retake logic deletes the regular entry and recreates it; deleted IDs are handled
            // gracefully by closeAll (entry-not-found adds to errors but returns 200).
            if (!entryIdsToClose.isEmpty()) {
                try {
                    closeEntries(entryIdsToClose, authHeader);
                } catch (Exception e) {
                    log.warn("Failed to close entries for student {}: {}", studentRow.getFullName(), e.getMessage());
                }
            }
        }

        long disciplinesWithProfessor = professorByDiscipline.values().stream()
                .filter(Objects::nonNull).count();

        return buildResult(report, (int) disciplinesWithProfessor, studentsMatched,
                gradesCreated, unmatchedStudents, errors);
    }

    private GroupDTO findGroup(String name, String authHeader) {
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

    private List<GroupMemberDTO> getGroupMembers(Long groupId, String authHeader) {
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

    private Long resolveSpecialtyId(Long bookNumberId, String authHeader) {
        try {
            BookNumberDTO book = restClient.get()
                    .uri(userServiceUrl + "/api/books/{id}", bookNumberId)
                    .header("Authorization", authHeader)
                    .retrieve()
                    .body(BookNumberDTO.class);
            return book != null ? book.getSpecialtyId() : null;
        } catch (Exception e) {
            log.error("Failed to get book number {}: {}", bookNumberId, e.getMessage());
            return null;
        }
    }

    private Long resolveSpecialtyIdByName(String specialtyName, String authHeader) {
        if (specialtyName == null || specialtyName.isBlank()) return null;
        try {
            String normalized = specialtyName.trim().toLowerCase();
            PageResponse<org.bachelor.integrationservice.model.client.SpecialtyDTO> page = restClient.get()
                    .uri(orgServiceUrl + "/api/specialties?size=200")
                    .header("Authorization", authHeader)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});
            if (page == null || page.getContent() == null) return null;
            return page.getContent().stream()
                    .filter(s -> matches(s.getNameUA(), normalized)
                            || matches(s.getStudyProgramUA(), normalized)
                            || matches(s.getEduProgramUA(), normalized))
                    .map(org.bachelor.integrationservice.model.client.SpecialtyDTO::getId)
                    .findFirst().orElse(null);
        } catch (Exception e) {
            log.error("Failed to resolve specialty by name '{}': {}", specialtyName, e.getMessage());
            return null;
        }
    }

    private static boolean matches(String field, String normalizedQuery) {
        return field != null && field.trim().toLowerCase().equals(normalizedQuery);
    }

    private List<SpecialtyDisciplineDTO> resolveSpecialtyDisciplines(
            List<ParsedDiscipline> disciplines, Long specialtyId, String academicYear,
            String authHeader, List<ImportErrorDTO> errors) {

        List<DisciplineDTO> allDisciplines = getAllDisciplines(authHeader);
        Map<String, DisciplineDTO> disciplineByName = new HashMap<>();
        for (DisciplineDTO d : allDisciplines) {
            disciplineByName.put(d.getName().trim(), d);
        }

        List<SpecialtyDisciplineDTO> result = new ArrayList<>();
        for (ParsedDiscipline parsed : disciplines) {
            try {
                String name = parsed.getName();
                // "Математика, повторний курс" → use base discipline, grade is a 2nd attempt
                String effectiveName = stripRetakeSuffix(name);
                if (!effectiveName.equals(name)) {
                    log.info("Retake discipline detected: '{}' → using base '{}'", name, effectiveName);
                }

                DisciplineDTO discipline = disciplineByName.get(effectiveName);
                SpecialtyDisciplineDTO sd;
                if (discipline == null) {
                    // Create discipline + specialty-discipline + hours in one call
                    DisciplineCreateResponseDTO created = createDisciplineWithSD(
                            effectiveName, specialtyId, academicYear, parsed, authHeader);
                    DisciplineDTO newDisc = new DisciplineDTO();
                    newDisc.setId(created.getDisciplineId());
                    newDisc.setName(created.getName());
                    disciplineByName.put(effectiveName, newDisc);
                    sd = new SpecialtyDisciplineDTO();
                    sd.setId(created.getSpecialtyDisciplineId());
                } else {
                    sd = findSpecialtyDiscipline(specialtyId, discipline.getId(), authHeader);
                    if (sd == null) {
                        sd = createSpecialtyDisciplineWithHours(
                                specialtyId, discipline.getId(), academicYear, parsed, authHeader);
                    }
                }
                result.add(sd);
            } catch (Exception e) {
                log.error("Failed to resolve specialty-discipline for {}: {}", parsed.getName(), e.getMessage());
                errors.add(new ImportErrorDTO(null, parsed.getName(), "Помилка дисципліни: " + e.getMessage()));
                result.add(null);
            }
        }
        return result;
    }

    /** Strips retake suffixes like "повторний курс", "повторне вивчення" etc. (case-insensitive). */
    private static String stripRetakeSuffix(String name) {
        return name.replaceAll("(?iU)[,.]?\\s*\\(?повторн[еий]+\\s+(?:курс|вивчення)\\)?\\s*$", "").trim();
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
            log.error("Failed to get disciplines: {}", e.getMessage());
            return List.of();
        }
    }

    private DisciplineCreateResponseDTO createDisciplineWithSD(
            String name, Long specialtyId, String academicYear,
            ParsedDiscipline parsed, String authHeader) {
        String year = (academicYear != null && !academicYear.isBlank()) ? academicYear : defaultAcademicYear();
        Map<String, Object> hours = buildHoursBody(year, parsed);

        Map<String, Object> body = new HashMap<>();
        body.put("name", name);
        body.put("specialtyId", specialtyId);
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
            Long specialtyId, Long disciplineId, String academicYear,
            ParsedDiscipline parsed, String authHeader) {
        // Create the specialty-discipline link, then add hours for this academic year
        SpecialtyDisciplineDTO sd = createSpecialtyDiscipline(specialtyId, disciplineId, authHeader);
        try {
            String year = (academicYear != null && !academicYear.isBlank()) ? academicYear : defaultAcademicYear();
            Map<String, Object> hours = buildHoursBody(year, parsed);
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

    private Map<String, Object> buildHoursBody(String academicYear, ParsedDiscipline parsed) {
        Map<String, Object> hours = new HashMap<>();
        hours.put("academicYear", academicYear);
        hours.put("totalHours", parsed.getTotalHours());
        hours.put("ectsCredits", parsed.getEctsCredits());
        hours.put("classroomHours", 0);
        return hours;
    }

    private SpecialtyDisciplineDTO findSpecialtyDiscipline(Long specialtyId, Long disciplineId, String authHeader) {
        try {
            List<SpecialtyDisciplineDTO> list = restClient.get()
                    .uri(gradeServiceUrl + "/api/specialty-disciplines?specialtyId={s}&disciplineId={d}",
                            specialtyId, disciplineId)
                    .header("Authorization", authHeader)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});
            return (list != null && !list.isEmpty()) ? list.get(0) : null;
        } catch (Exception e) {
            return null;
        }
    }

    private SpecialtyDisciplineDTO createSpecialtyDiscipline(Long specialtyId, Long disciplineId, String authHeader) {
        return restClient.post()
                .uri(gradeServiceUrl + "/api/specialty-disciplines/{specialtyId}?disciplineId={disciplineId}",
                        specialtyId, disciplineId)
                .header("Authorization", authHeader)
                .retrieve()
                .body(SpecialtyDisciplineDTO.class);
    }

    private GradeBookEntryDTO findOrCreateEntry(Long bookNumberId, Long sdId, Long professorId,
                                                String academicYear, String authHeader) {
        PageResponse<GradeBookEntryDTO> page = restClient.get()
                .uri(gradeServiceUrl + "/api/records?bookNumberId={b}&specialtyDisciplineId={sd}&academicYear={y}&size=200",
                        bookNumberId, sdId, academicYear)
                .header("Authorization", authHeader)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
        List<GradeBookEntryDTO> existing = (page != null && page.getContent() != null) ? page.getContent() : List.of();

        if (!existing.isEmpty()) return existing.get(0);

        Map<String, Object> body = new HashMap<>();
        body.put("specialtyDisciplineId", sdId);
        body.put("professorId", professorId);
        body.put("academicYear", academicYear);
        body.put("bookNumberIds", List.of(bookNumberId));

        List<GradeBookEntryDTO> created = restClient.post()
                .uri(gradeServiceUrl + "/api/records")
                .header("Authorization", authHeader)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});

        if (created == null || created.isEmpty()) {
            throw new IllegalStateException("Failed to create grade book entry");
        }
        return created.get(0);
    }

    private GradeBookEntryDTO findOrCreateRetakeEntry(Long bookNumberId, Long sdId,
                                                       Long professorId, String academicYear,
                                                       String authHeader) {
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
                // Entry has no grade yet — delete it and recreate at the correct attempt number
                // so we don't create a phantom attempt-1 before entering the retake grade.
                int targetAttempt = Math.max(2, prev.getAttempt() != null ? prev.getAttempt() : 1);
                String year = prev.getAcademicYear() != null ? prev.getAcademicYear() : academicYear;
                restClient.delete()
                        .uri(gradeServiceUrl + "/api/records/{id}", prev.getId())
                        .header("Authorization", authHeader)
                        .retrieve()
                        .toBodilessEntity();
                log.info("Deleted empty IN_PROGRESS entry {} for retake; will create attempt {}",
                        prev.getId(), targetAttempt);
                return createEntryWithMinAttempt(bookNumberId, sdId, professorId, year, targetAttempt, authHeader);
            }

            // Previous entry is COMPLETED (FAILED) — create retake via dedicated endpoint
            return restClient.post()
                    .uri(gradeServiceUrl + "/api/records/{id}/retake?professorId={p}",
                            prev.getId(), professorId)
                    .header("Authorization", authHeader)
                    .retrieve()
                    .body(GradeBookEntryDTO.class);
        }

        // No previous entry at all — create directly as attempt 2
        log.warn("Retake discipline has no prior entry for bookNumberId={}, sdId={} — creating as attempt 2",
                bookNumberId, sdId);
        return createEntryWithMinAttempt(bookNumberId, sdId, professorId, academicYear, 2, authHeader);
    }

    private GradeBookEntryDTO createEntryWithMinAttempt(Long bookNumberId, Long sdId, Long professorId,
                                                         String academicYear, int minAttempt,
                                                         String authHeader) {
        Map<String, Object> body = new HashMap<>();
        body.put("specialtyDisciplineId", sdId);
        body.put("professorId", professorId);
        body.put("academicYear", academicYear != null ? academicYear : defaultAcademicYear());
        body.put("bookNumberIds", List.of(bookNumberId));
        body.put("minAttempt", minAttempt);
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

    private void createGrade(Long entryId, ParsedGrade grade, String authHeader) {
        String assessmentType = resolveAssessmentType(grade.getNationalGrade());
        Map<String, Object> body = new HashMap<>();
        body.put("entryId", entryId);
        body.put("assessmentDate", LocalDateTime.now().toString());
        body.put("assessmentType", assessmentType);
        if (grade.getUniversityGrade() != null) {
            body.put("universityGrade", grade.getUniversityGrade());
        }
        restClient.post()
                .uri(gradeServiceUrl + "/api/grades")
                .header("Authorization", authHeader)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .toBodilessEntity();
    }

    private void closeEntries(List<Long> entryIds, String authHeader) {
        Map<String, Object> body = new HashMap<>();
        body.put("entryIds", entryIds);
        restClient.patch()
                .uri(gradeServiceUrl + "/api/records/close")
                .header("Authorization", authHeader)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .toBodilessEntity();
    }

    private String resolveAssessmentType(Object nationalGrade) {
        if (nationalGrade instanceof String s && ("Зар.".equals(s) || "Нзр.".equals(s))) return "CREDIT";
        return "EXAM";
    }

    /**
     * Searches all STUDENT users in user-service, matches them to the names from the file,
     * finds their active book numbers and adds them to the group.
     */
    private List<GroupMemberDTO> findAndAddGroupMembers(Long groupId,
                                                        List<ParsedStudentRow> students,
                                                        String authHeader,
                                                        List<ImportErrorDTO> errors) {
        List<UserDTO> allStudents = getAllStudents(authHeader);
        List<GroupMemberDTO> result = new ArrayList<>();

        for (ParsedStudentRow studentRow : students) {
            String[] parts = normalizeName(studentRow.getFullName()).split("\\s+");
            if (parts.length == 0 || parts[0].isBlank()) continue;
            String lastName = parts[0].toLowerCase();
            String firstInitial = parts.length > 1 ? parts[1].replace(".", "").toLowerCase() : null;

            UserDTO matched = allStudents.stream()
                    .filter(u -> u.getLastName() != null && u.getLastName().toLowerCase().equals(lastName))
                    .filter(u -> firstInitial == null || u.getFirstName() == null ||
                            u.getFirstName().substring(0, 1).toLowerCase().equals(firstInitial))
                    .findFirst().orElse(null);

            if (matched == null) {
                log.warn("Student not found in system: {}", studentRow.getFullName());
                errors.add(new ImportErrorDTO(studentRow.getFullName(), null,
                        "Студента не знайдено в системі"));
                continue;
            }

            List<BookNumberDTO> books = getStudentBooks(matched.getId(), authHeader);
            BookNumberDTO activeBook = books.stream()
                    .filter(b -> "REGISTERED".equals(b.getStatus()) || "FILLED".equals(b.getStatus()))
                    .findFirst().orElse(null);

            if (activeBook == null) {
                log.warn("No active book number for student: {}", studentRow.getFullName());
                errors.add(new ImportErrorDTO(studentRow.getFullName(), null,
                        "Немає активної залікової книжки"));
                continue;
            }

            try {
                GroupMemberDTO member = addMemberToGroup(groupId, activeBook.getId(), authHeader);
                if (member != null) result.add(member);
            } catch (Exception e) {
                log.warn("Could not add {} to group (may already be a member): {}",
                        studentRow.getFullName(), e.getMessage());
                // Build a synthetic member so import can still proceed
                GroupMemberDTO synthetic = new GroupMemberDTO();
                synthetic.setBookNumberId(activeBook.getId());
                synthetic.setStudentId(matched.getId());
                synthetic.setStudentName(matched.getLastName() + " " + matched.getFirstName());
                result.add(synthetic);
            }
        }

        return result;
    }

    private List<UserDTO> getAllStudents(String authHeader) {
        try {
            List<UserDTO> users = restClient.get()
                    .uri(userServiceUrl + "/api/users")
                    .header("Authorization", authHeader)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});
            if (users == null) return List.of();
            return users.stream()
                    .filter(u -> "STUDENT".equals(u.getRole()))
                    .toList();
        } catch (Exception e) {
            log.error("Failed to get users: {}", e.getMessage());
            return List.of();
        }
    }

    private List<BookNumberDTO> getStudentBooks(Long studentId, String authHeader) {
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

    private GroupMemberDTO addMemberToGroup(Long groupId, Long bookNumberId, String authHeader) {
        return restClient.post()
                .uri(userServiceUrl + "/api/groups/{groupId}/members/{bookNumberId}",
                        groupId, bookNumberId)
                .header("Authorization", authHeader)
                .retrieve()
                .body(GroupMemberDTO.class);
    }

    private static String normalizeName(String s) {
        return s.replaceAll("[\\p{Z}\\s]+", " ").trim();
    }

    private GroupMemberDTO matchStudent(String parsedName, List<GroupMemberDTO> members) {
        String[] parts = normalizeName(parsedName).split("\\s+");
        if (parts.length == 0 || parts[0].isBlank()) return null;
        String lastName = parts[0].toLowerCase();
        String firstInitial = parts.length > 1 ? parts[1].replace(".", "").toLowerCase() : null;

        for (GroupMemberDTO m : members) {
            if (m.getStudentName() == null) continue;
            String[] nameParts = normalizeName(m.getStudentName()).split("\\s+");
            if (nameParts.length < 1) continue;
            if (!nameParts[0].toLowerCase().equals(lastName)) continue;
            if (firstInitial == null || nameParts.length < 2) return m;
            if (nameParts[1].substring(0, 1).toLowerCase().equals(firstInitial)) return m;
        }
        return null;
    }

    private String defaultAcademicYear() {
        int year = java.time.LocalDate.now().getYear();
        return year + "/" + (year + 1);
    }

    private ImportResultDTO buildResult(ParsedReport report, int disciplinesProcessed,
                                        int studentsMatched, int gradesCreated,
                                        List<String> unmatched, List<ImportErrorDTO> errors) {
        return ImportResultDTO.builder()
                .groupName(report.getGroupName())
                .academicYear(report.getAcademicYear())
                .disciplinesProcessed(disciplinesProcessed)
                .studentsMatched(studentsMatched)
                .studentsUnmatched(unmatched.size())
                .gradesCreated(gradesCreated)
                .unmatchedStudents(unmatched)
                .errors(errors)
                .build();
    }
}
