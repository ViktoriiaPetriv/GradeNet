package org.bachelor.integrationservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bachelor.integrationservice.model.ParsedDiscipline;
import org.bachelor.integrationservice.model.ParsedGrade;
import org.bachelor.integrationservice.model.ParsedReport;
import org.bachelor.integrationservice.model.ParsedStudentRow;
import org.bachelor.integrationservice.model.client.*;
import org.bachelor.integrationservice.model.dto.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
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

    public DisciplineCheckResultDTO checkDisciplines(InputStream fileInputStream, String authHeader) throws IOException {
        ParsedReport report = excelParserService.parse(fileInputStream);
        String academicYear = (report.getAcademicYear() != null && !report.getAcademicYear().isBlank())
                ? report.getAcademicYear() : defaultAcademicYear();

        org.bachelor.integrationservice.model.client.SpecialtyDTO specialty =
                resolveSpecialtyByName(report.getSpecialtyName(), authHeader);
        Long specialtyId = specialty != null ? specialty.getId() : null;
        Integer graduationYear = specialty != null
                ? ExcelParserService.calculateGraduationYear(academicYear, getFirstSemester(report), specialty.getDegree())
                : null;
        Long specialtyOfferingId = (specialtyId != null && graduationYear != null)
                ? resolveOfferingIdBySpecialtyId(specialtyId, graduationYear, authHeader)
                : null;
        if (specialtyOfferingId == null) {
            log.warn("Could not resolve specialty offering for '{}' graduation year {}", report.getSpecialtyName(), graduationYear);
        }

        List<DisciplineDTO> allDisciplines = getAllDisciplines(authHeader);
        Map<String, DisciplineDTO> disciplineByName = new HashMap<>();
        for (DisciplineDTO d : allDisciplines) {
            disciplineByName.put(d.getName().trim(), d);
        }

        List<DisciplineCheckItemDTO> disciplineItems = new ArrayList<>();
        for (ParsedDiscipline parsed : report.getDisciplines()) {
            String name = parsed.getName();
            String effectiveName = stripRetakeSuffix(name);

            DisciplineDTO existingDiscipline = disciplineByName.get(effectiveName);
            Long disciplineId = existingDiscipline != null ? existingDiscipline.getId() : null;
            Long specialtyDisciplineId = null;

            if (existingDiscipline != null && specialtyOfferingId != null) {
                SpecialtyDisciplineDTO sd = findSpecialtyDiscipline(specialtyOfferingId, disciplineId, authHeader);
                specialtyDisciplineId = sd != null ? sd.getId() : null;
            }

            disciplineItems.add(DisciplineCheckItemDTO.builder()
                    .index(report.getDisciplines().indexOf(parsed))
                    .name(parsed.getName())
                    .totalHours(parsed.getTotalHours())
                    .ectsCredits(parsed.getEctsCredits())
                    .existsInSystem(existingDiscipline != null)
                    .disciplineId(disciplineId)
                    .specialtyDisciplineId(specialtyDisciplineId)
                    .semester(parsed.getSemester())
                    .build());
        }

        return DisciplineCheckResultDTO.builder()
                .groupName(report.getGroupName())
                .academicYear(academicYear)
                .specialtyName(report.getSpecialtyName())
                .specialtyId(specialtyId)
                .graduationYear(graduationYear)
                .specialtyOfferingId(specialtyOfferingId)
                .disciplines(disciplineItems)
                .build();
    }

    public List<CreatedDisciplineInfoDTO> createDisciplines(
            InputStream fileInputStream, List<Integer> indices, Long specialtyOfferingId,
            String academicYear, String authHeader) throws IOException {
        ParsedReport report = excelParserService.parse(fileInputStream);
        String year = (academicYear != null && !academicYear.isBlank()) ? academicYear : defaultAcademicYear();

        List<CreatedDisciplineInfoDTO> result = new ArrayList<>();
        for (int idx : indices) {
            if (idx < 0 || idx >= report.getDisciplines().size()) continue;

            ParsedDiscipline parsed = report.getDisciplines().get(idx);
            String effectiveName = stripRetakeSuffix(parsed.getName());

            try {
                DisciplineCreateResponseDTO created = createDisciplineWithSD(
                        effectiveName, specialtyOfferingId, year, parsed, authHeader);
                result.add(CreatedDisciplineInfoDTO.builder()
                        .index(idx)
                        .disciplineId(created.getDisciplineId())
                        .specialtyDisciplineId(created.getSpecialtyDisciplineId())
                        .build());
                log.info("Created discipline at index {}: {} (id={})", idx, effectiveName, created.getDisciplineId());
            } catch (Exception e) {
                log.error("Failed to create discipline at index {}: {}", idx, e.getMessage());
                throw new RuntimeException("Failed to create discipline: " + effectiveName, e);
            }
        }
        return result;
    }

    public StudentCheckResultDTO checkStudents(InputStream fileInputStream, String authHeader) throws IOException {
        ParsedReport report = excelParserService.parse(fileInputStream);

        GroupDTO group = findGroup(report.getGroupName(), authHeader);
        if (group == null) {
            log.warn("Group not found: {}", report.getGroupName());
            return StudentCheckResultDTO.builder()
                    .groupName(report.getGroupName())
                    .students(new ArrayList<>())
                    .build();
        }

        List<GroupMemberDTO> members = new ArrayList<>(getGroupMembers(group.getId(), authHeader));
        if (members.isEmpty()) {
            log.info("Group {} has no members, searching system students by name", group.getName());
            List<ImportErrorDTO> errors = new ArrayList<>();
            members = findAndAddGroupMembers(group.getId(), report.getStudents(), authHeader, errors);
        }

        // Resolve report graduation year for per-student validation
        String academicYear = (report.getAcademicYear() != null && !report.getAcademicYear().isBlank())
                ? report.getAcademicYear() : defaultAcademicYear();
        org.bachelor.integrationservice.model.client.SpecialtyDTO specialty =
                resolveSpecialtyByName(report.getSpecialtyName(), authHeader);
        Integer reportGradYear = ExcelParserService.calculateGraduationYear(
                academicYear, getFirstSemester(report), specialty != null ? specialty.getDegree() : null);
        Long reportOfferingId = specialty != null
                ? resolveOfferingIdBySpecialtyId(specialty.getId(), reportGradYear, authHeader)
                : null;

        List<StudentCheckItemDTO> studentItems = new ArrayList<>();
        for (ParsedStudentRow studentRow : report.getStudents()) {
            GroupMemberDTO member = matchStudent(studentRow.getFullName(), members);

            boolean graduationYearMismatch = false;
            if (member != null && reportOfferingId != null) {
                Long studentOfferingId = resolveSpecialtyOfferingId(member.getBookNumberId(), authHeader);
                graduationYearMismatch = !reportOfferingId.equals(studentOfferingId);
                if (graduationYearMismatch) {
                    Integer studentGradYear = getOfferingGraduationYear(studentOfferingId, authHeader);
                    log.warn("Graduation year mismatch for {}: student={}, report={}",
                            studentRow.getFullName(), studentGradYear, reportGradYear);
                }
            }

            List<GradeDataDTO> gradeData = studentRow.getGrades().stream()
                    .map(g -> GradeDataDTO.builder()
                            .disciplineIndex(g.getDisciplineIndex())
                            .universityGrade(g.getUniversityGrade())
                            .ectsGrade(g.getEctsGrade())
                            .nationalGrade(g.getNationalGrade() != null ? g.getNationalGrade().toString() : null)
                            .build())
                    .toList();

            studentItems.add(StudentCheckItemDTO.builder()
                    .fullName(studentRow.getFullName())
                    .bookNumberId(member != null ? member.getBookNumberId() : null)
                    .studentId(member != null ? member.getStudentId() : null)
                    .existsInSystem(member != null)
                    .graduationYearMismatch(graduationYearMismatch)
                    .grades(gradeData)
                    .build());
        }

        return StudentCheckResultDTO.builder()
                .groupName(report.getGroupName())
                .students(studentItems)
                .build();
    }

    /**
     * @param professorByDiscipline map of discipline index → professorId;
     *                              disciplines absent from the map are skipped
     * @param selectedStudentBookNumberIds if provided, only these students will have grades imported;
     *                                     if null, all matched students are imported (backward-compat)
     */
    public ImportResultDTO importGradeReport(MultipartFile file,
                                             Map<Integer, Long> professorByDiscipline,
                                             List<Long> selectedStudentBookNumberIds,
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

        // Resolve target specialty offering from report metadata (correct graduation year by degree)
        Long targetOfferingId = resolveSpecialtyOfferingIdByName(
                report.getSpecialtyName(), academicYear, getFirstSemester(report), authHeader);

        // Pre-fetch per-member offering IDs for graduation year validation
        Map<Long, Long> memberOfferingMap = new HashMap<>();
        for (GroupMemberDTO m : members) {
            Long sid = resolveSpecialtyOfferingId(m.getBookNumberId(), authHeader);
            if (sid != null) memberOfferingMap.put(m.getBookNumberId(), sid);
        }

        // Prefer report-derived offering; fall back to first member's offering
        Long specialtyOfferingId = targetOfferingId != null
                ? targetOfferingId
                : memberOfferingMap.get(members.get(0).getBookNumberId());
        if (specialtyOfferingId == null) {
            errors.add(new ImportErrorDTO(null, null, "Не вдалося визначити спеціальність із залікової книжки студента"));
            return buildResult(report, 0, 0, 0, unmatchedStudents, errors);
        }

        // Pre-fetch graduation year of the target offering for helpful error messages
        final Integer reportGradYear = getOfferingGraduationYear(specialtyOfferingId, authHeader);

        List<SpecialtyDisciplineDTO> specialtyDisciplines =
                resolveSpecialtyDisciplines(report.getDisciplines(), specialtyOfferingId,
                        academicYear, professorByDiscipline.keySet(), authHeader, errors);

        int studentsMatched = 0;
        int gradesCreated = 0;

        for (ParsedStudentRow studentRow : report.getStudents()) {
            GroupMemberDTO member = matchStudent(studentRow.getFullName(), members);
            if (member == null) {
                unmatchedStudents.add(studentRow.getFullName());
                log.warn("Could not match student: {}", studentRow.getFullName());
                continue;
            }

            if (selectedStudentBookNumberIds != null && !selectedStudentBookNumberIds.contains(member.getBookNumberId())) {
                log.debug("Student {} skipped (not in selectedStudentBookNumberIds)", studentRow.getFullName());
                continue;
            }

            Long studentOfferingId = memberOfferingMap.get(member.getBookNumberId());
            if (!specialtyOfferingId.equals(studentOfferingId)) {
                Integer studentGradYear = getOfferingGraduationYear(studentOfferingId, authHeader);
                String msg = studentGradYear != null && reportGradYear != null
                        ? String.format("Студент належить до випуску %d, але звіт для випуску %d",
                                studentGradYear, reportGradYear)
                        : "Студент належить до іншого випуску спеціальності";
                log.warn("Graduation year mismatch for {}: student={}, report={}",
                        studentRow.getFullName(), studentGradYear, reportGradYear);
                errors.add(new ImportErrorDTO(studentRow.getFullName(), null, msg));
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
                    Integer disciplineSemester = idx < report.getDisciplines().size()
                            ? report.getDisciplines().get(idx).getSemester() : null;
                    GradeBookEntryDTO entry = isRetake
                            ? findOrCreateRetakeEntry(member.getBookNumberId(), sd.getId(), professorId, academicYear, disciplineSemester, authHeader)
                            : findOrCreateEntry(member.getBookNumberId(), sd.getId(), professorId, academicYear, disciplineSemester, authHeader);
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

    private Long resolveSpecialtyOfferingId(Long bookNumberId, String authHeader) {
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

    private org.bachelor.integrationservice.model.client.SpecialtyDTO resolveSpecialtyByName(
            String specialtyName, String authHeader) {
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
                    .findFirst().orElse(null);
        } catch (Exception e) {
            log.error("Failed to resolve specialty by name '{}': {}", specialtyName, e.getMessage());
            return null;
        }
    }

    private Long resolveSpecialtyOfferingIdByName(String specialtyName, String academicYear,
                                                   Integer semester, String authHeader) {
        if (specialtyName == null || specialtyName.isBlank()) return null;
        try {
            org.bachelor.integrationservice.model.client.SpecialtyDTO specialty =
                    resolveSpecialtyByName(specialtyName, authHeader);
            if (specialty == null) return null;
            Integer graduationYear = ExcelParserService.calculateGraduationYear(
                    academicYear, semester, specialty.getDegree());
            return resolveOfferingIdBySpecialtyId(specialty.getId(), graduationYear, authHeader);
        } catch (Exception e) {
            log.error("Failed to resolve specialty offering by name '{}': {}", specialtyName, e.getMessage());
            return null;
        }
    }

    private Integer getOfferingGraduationYear(Long offeringId, String authHeader) {
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

    private Long resolveOfferingIdBySpecialtyId(Long specialtyId, Integer graduationYear, String authHeader) {
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

    private static boolean matches(String field, String normalizedQuery) {
        return field != null && field.trim().toLowerCase().equals(normalizedQuery);
    }

    private List<SpecialtyDisciplineDTO> resolveSpecialtyDisciplines(
            List<ParsedDiscipline> disciplines, Long specialtyOfferingId, String academicYear,
            Set<Integer> selectedIndices, String authHeader, List<ImportErrorDTO> errors) {

        List<DisciplineDTO> allDisciplines = getAllDisciplines(authHeader);
        Map<String, DisciplineDTO> disciplineByName = new HashMap<>();
        for (DisciplineDTO d : allDisciplines) {
            disciplineByName.put(d.getName().trim(), d);
        }

        List<SpecialtyDisciplineDTO> result = new ArrayList<>();
        for (int idx = 0; idx < disciplines.size(); idx++) {
            if (!selectedIndices.contains(idx)) {
                result.add(null);
                continue;
            }
            ParsedDiscipline parsed = disciplines.get(idx);
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
                            effectiveName, specialtyOfferingId, academicYear, parsed, authHeader);
                    DisciplineDTO newDisc = new DisciplineDTO();
                    newDisc.setId(created.getDisciplineId());
                    newDisc.setName(created.getName());
                    disciplineByName.put(effectiveName, newDisc);
                    sd = new SpecialtyDisciplineDTO();
                    sd.setId(created.getSpecialtyDisciplineId());
                } else {
                    sd = findSpecialtyDiscipline(specialtyOfferingId, discipline.getId(), authHeader);
                    if (sd == null) {
                        sd = createSpecialtyDisciplineWithHours(
                                specialtyOfferingId, discipline.getId(), academicYear, parsed, authHeader);
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
            String name, Long specialtyOfferingId, String academicYear,
            ParsedDiscipline parsed, String authHeader) {
        String year = (academicYear != null && !academicYear.isBlank()) ? academicYear : defaultAcademicYear();
        Map<String, Object> hours = buildHoursBody(year, parsed);

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

    private SpecialtyDisciplineDTO createSpecialtyDisciplineWithHours(
            Long specialtyOfferingId, Long disciplineId, String academicYear,
            ParsedDiscipline parsed, String authHeader) {
        // Create the specialty-discipline link, then add hours for this academic year
        SpecialtyDisciplineDTO sd = createSpecialtyDiscipline(specialtyOfferingId, disciplineId, authHeader);
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

    private SpecialtyDisciplineDTO findSpecialtyDiscipline(Long specialtyOfferingId, Long disciplineId, String authHeader) {
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

    private SpecialtyDisciplineDTO createSpecialtyDiscipline(Long specialtyOfferingId, Long disciplineId, String authHeader) {
        return restClient.post()
                .uri(gradeServiceUrl + "/api/specialty-disciplines/{specialtyOfferingId}?disciplineId={disciplineId}",
                        specialtyOfferingId, disciplineId)
                .header("Authorization", authHeader)
                .retrieve()
                .body(SpecialtyDisciplineDTO.class);
    }

    private GradeBookEntryDTO findOrCreateEntry(Long bookNumberId, Long sdId, Long professorId,
                                                String academicYear, Integer semester, String authHeader) {
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
        if (semester != null) body.put("semester", semester);

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
                return createEntryWithMinAttempt(bookNumberId, sdId, professorId, year, targetAttempt, semester, authHeader);
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
        return createEntryWithMinAttempt(bookNumberId, sdId, professorId, academicYear, 2, semester, authHeader);
    }

    private GradeBookEntryDTO createEntryWithMinAttempt(Long bookNumberId, Long sdId, Long professorId,
                                                         String academicYear, int minAttempt,
                                                         Integer semester, String authHeader) {
        Map<String, Object> body = new HashMap<>();
        body.put("specialtyDisciplineId", sdId);
        body.put("professorId", professorId);
        body.put("academicYear", academicYear != null ? academicYear : defaultAcademicYear());
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

    private static Integer getFirstSemester(ParsedReport report) {
        return report.getDisciplines().stream()
                .map(ParsedDiscipline::getSemester)
                .filter(Objects::nonNull)
                .findFirst().orElse(null);
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
