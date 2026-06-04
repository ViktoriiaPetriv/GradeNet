package org.bachelor.integrationservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bachelor.integrationservice.mapper.JournalStatusMapper;
import org.bachelor.integrationservice.model.client.*;
import org.bachelor.integrationservice.model.dto.*;
import org.bachelor.integrationservice.model.journal.*;
import org.bachelor.integrationservice.service.client.GradeServiceClient;
import org.bachelor.integrationservice.service.client.UserServiceClient;
import org.bachelor.integrationservice.service.journal.JournalClient;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class JournalImportService {

    private final JournalClient journalClient;
    private final JournalStatusMapper journalStatusMapper;
    private final GradeServiceClient gradeClient;
    private final UserServiceClient userClient;

    public List<JournalStudentStatusDTO> getStudentsWithStatus(long specialtyId, String authHeader) {
        List<JournalStudentDTO> journalStudents = journalClient.getStudents(specialtyId);
        Map<String, UserDTO> systemUserByEmail = userClient.getAllStudents(authHeader).stream()
                .filter(u -> u.getEmail() != null)
                .collect(Collectors.toMap(u -> u.getEmail().toLowerCase(), u -> u, (a, b) -> a));

        return journalStudents.stream().map(s -> {
            JournalStudentStatusDTO dto = journalStatusMapper.toStudentStatus(s);
            dto.setExistsInSystem(s.getEmail() != null
                    && systemUserByEmail.containsKey(s.getEmail().toLowerCase()));
            return dto;
        }).collect(Collectors.toList());
    }

    public List<JournalDisciplineStatusDTO> getDisciplinesWithStatus(long specialtyId, String authHeader) {
        List<JournalDisciplineDTO> journalDiscs = journalClient.getDisciplines(specialtyId);
        Map<String, DisciplineDTO> disciplineByName = gradeClient.getAllDisciplines(authHeader).stream()
                .collect(Collectors.toMap(d -> normKey(d.getName()), d -> d, (a, b) -> a));

        return journalDiscs.stream().map(d -> {
            JournalDisciplineStatusDTO dto = journalStatusMapper.toDisciplineStatus(d);
            dto.setExistsInSystem(disciplineByName.containsKey(normKey(d.getName())));
            try {
                JournalDisciplineDetailDTO detail = journalClient.getDisciplineDetail(d.getExternalId());
                dto.setSemester(detail.getSemester());
                dto.setTotalHours(detail.getTotalHours());
                dto.setAcademicYear(detail.getAcademicYear());
                List<Integer> attempts = detail.getGrades().stream()
                        .map(JournalStudentGradeDTO::getAttempt)
                        .filter(Objects::nonNull)
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
        Map<Long, Map<Integer, Map<Long, Long>>> overrides =
                request.getProfessorOverridesByStudent() != null
                        ? request.getProfessorOverridesByStudent() : Map.of();

        Set<Long> selectedStudentIds = request.getSelectedStudentExternalIds() != null
                && !request.getSelectedStudentExternalIds().isEmpty()
                ? new HashSet<>(request.getSelectedStudentExternalIds()) : null;

        Map<Long, JournalStudentDTO> journalStudentById = journalClient.getStudents(specialtyId).stream()
                .collect(Collectors.toMap(JournalStudentDTO::getExternalId, s -> s));
        Map<Long, String> disciplineNameById = journalClient.getDisciplines(specialtyId).stream()
                .collect(Collectors.toMap(JournalDisciplineDTO::getExternalId, JournalDisciplineDTO::getName, (a, b) -> a));
        Map<String, UserDTO> systemUserByEmail = userClient.getAllStudents(authHeader).stream()
                .filter(u -> u.getEmail() != null)
                .collect(Collectors.toMap(u -> u.getEmail().toLowerCase(), u -> u, (a, b) -> a));
        Map<String, DisciplineDTO> disciplineByName = new HashMap<>();
        for (DisciplineDTO d : gradeClient.getAllDisciplines(authHeader)) {
            disciplineByName.put(normKey(d.getName()), d);
        }

        List<String> unmatchedStudents = new ArrayList<>();
        List<ImportErrorDTO> errors = new ArrayList<>();
        Set<Long> matchedUserIds = new HashSet<>();
        int disciplinesProcessed = 0;
        int gradesCreated = 0;

        for (Map.Entry<Long, Map<Integer, Long>> entry : professorMap.entrySet()) {
            Map<Integer, Map<Long, Long>> disciplineOverrides =
                    overrides.getOrDefault(entry.getKey(), Map.of());
            int[] counts = processDisciplineEntry(entry.getKey(), entry.getValue(), disciplineOverrides,
                    disciplineNameById, disciplineByName, offeringId, academicYear,
                    selectedStudentIds, journalStudentById, systemUserByEmail,
                    matchedUserIds, unmatchedStudents, errors, authHeader);
            disciplinesProcessed += counts[0];
            gradesCreated += counts[1];
        }

        return JournalImportResultDTO.builder()
                .disciplinesProcessed(disciplinesProcessed)
                .studentsMatched(matchedUserIds.size())
                .studentsUnmatched(unmatchedStudents.size())
                .gradesCreated(gradesCreated)
                .unmatchedStudents(unmatchedStudents)
                .errors(errors)
                .build();
    }

    private UserDTO resolveOrCreateUser(JournalStudentDTO jStudent,
                                        Map<String, UserDTO> systemUserByEmail,
                                        String authHeader) {
        UserDTO user = systemUserByEmail.get(jStudent.getEmail().toLowerCase());
        if (user == null) {
            Map<String, Object> body = new HashMap<>();
            body.put("firstName", jStudent.getFirstName());
            body.put("lastName", jStudent.getLastName());
            body.put("patronymic", jStudent.getPatronymic());
            body.put("email", jStudent.getEmail());
            user = userClient.createStudent(body, authHeader);
            if (user != null) systemUserByEmail.put(user.getEmail().toLowerCase(), user);
        }
        return user;
    }

    private int[] processDisciplineEntry(
            long extDisciplineId, Map<Integer, Long> attemptProfessorMap,
            Map<Integer, Map<Long, Long>> studentOverrides,
            Map<Long, String> disciplineNameById, Map<String, DisciplineDTO> disciplineByName,
            long offeringId, String academicYear, Set<Long> selectedStudentIds,
            Map<Long, JournalStudentDTO> journalStudentById, Map<String, UserDTO> systemUserByEmail,
            Set<Long> matchedUserIds, List<String> unmatchedStudents,
            List<ImportErrorDTO> errors, String authHeader) {

        JournalDisciplineDetailDTO detail;
        try {
            detail = journalClient.getDisciplineDetail(extDisciplineId);
        } catch (Exception e) {
            log.error("Failed to fetch discipline {}: {}", extDisciplineId, e.getMessage());
            errors.add(new ImportErrorDTO(null, "external#" + extDisciplineId, "Не вдалося завантажити дисципліну"));
            return new int[]{0, 0};
        }
        detail.setName(disciplineNameById.getOrDefault(extDisciplineId, detail.getName()));

        SpecialtyDisciplineDTO sd;
        try {
            sd = resolveSpecialtyDiscipline(detail, offeringId, academicYear, disciplineByName, authHeader);
        } catch (Exception e) {
            log.error("Failed to resolve specialty-discipline '{}': {}", detail.getName(), e.getMessage());
            errors.add(new ImportErrorDTO(null, detail.getName(), "Помилка дисципліни: " + e.getMessage()));
            return new int[]{0, 0};
        }

        List<Long> entryIdsToClose = new ArrayList<>();
        int gradesCreated = processGradesForDiscipline(detail, sd, attemptProfessorMap, studentOverrides,
                selectedStudentIds, journalStudentById, systemUserByEmail,
                offeringId, academicYear, matchedUserIds, entryIdsToClose,
                unmatchedStudents, errors, authHeader);

        if (!entryIdsToClose.isEmpty()) {
            try {
                gradeClient.closeEntries(entryIdsToClose, authHeader);
            } catch (Exception e) {
                log.warn("Failed to close entries: {}", e.getMessage());
            }
        }
        return new int[]{1, gradesCreated};
    }

    private int processGradesForDiscipline(
            JournalDisciplineDetailDTO detail, SpecialtyDisciplineDTO sd,
            Map<Integer, Long> attemptProfessorMap, Map<Integer, Map<Long, Long>> studentOverrides,
            Set<Long> selectedStudentIds,
            Map<Long, JournalStudentDTO> journalStudentById, Map<String, UserDTO> systemUserByEmail,
            long offeringId, String academicYear, Set<Long> matchedUserIds,
            List<Long> entryIdsToClose, List<String> unmatchedStudents,
            List<ImportErrorDTO> errors, String authHeader) {

        int gradesCreated = 0;
        for (JournalStudentGradeDTO grade : detail.getGrades()) {
            if (selectedStudentIds != null && !selectedStudentIds.contains(grade.getStudentExternalId())) continue;

            int attempt = grade.getAttempt() != null ? grade.getAttempt() : 1;
            Long professorId = studentOverrides
                    .getOrDefault(attempt, Map.of())
                    .getOrDefault(grade.getStudentExternalId(), null);
            if (professorId == null) {
                professorId = attemptProfessorMap.getOrDefault(attempt,
                        attemptProfessorMap.values().stream().findFirst().orElse(null));
            }
            if (professorId == null) {
                errors.add(new ImportErrorDTO("externalId=" + grade.getStudentExternalId(),
                        detail.getName(), "Не вказано викладача для спроби " + attempt));
                continue;
            }

            JournalStudentDTO jStudent = journalStudentById.get(grade.getStudentExternalId());
            if (jStudent == null) {
                log.warn("No journal student for externalId={}", grade.getStudentExternalId());
                unmatchedStudents.add("externalId=" + grade.getStudentExternalId());
                continue;
            }

            UserDTO user = resolveOrCreateUser(jStudent, systemUserByEmail, authHeader);
            if (user == null) {
                String name = jStudent.getLastName() + " " + jStudent.getFirstName();
                errors.add(new ImportErrorDTO(name, detail.getName(),
                        "Не вдалося створити студента: " + jStudent.getEmail()));
                if (!unmatchedStudents.contains(name)) unmatchedStudents.add(name);
                continue;
            }

            BookNumberDTO book = userClient.findOrCreateBook(user.getId(), offeringId, authHeader);
            if (book == null) {
                String name = jStudent.getLastName() + " " + jStudent.getFirstName();
                errors.add(new ImportErrorDTO(name, detail.getName(),
                        "Не вдалося знайти або створити залікову книжку"));
                continue;
            }

            matchedUserIds.add(user.getId());
            try {
                GradeBookEntryDTO gbe = gradeClient.findOrCreateEntryWithAttempt(
                        book.getId(), sd.getId(), professorId, academicYear, detail.getSemester(), attempt, authHeader);
                gradeClient.createGrade(gbe.getId(), grade.getUniversityGrade(), grade.getAssessmentDate(), authHeader);
                entryIdsToClose.add(gbe.getId());
                gradesCreated++;
            } catch (Exception e) {
                String name = jStudent.getLastName() + " " + jStudent.getFirstName();
                log.error("Grade save failed for {} / {}: {}", name, detail.getName(), e.getMessage());
                errors.add(new ImportErrorDTO(name, detail.getName(), e.getMessage()));
            }
        }
        return gradesCreated;
    }

    private SpecialtyDisciplineDTO resolveSpecialtyDiscipline(
            JournalDisciplineDetailDTO detail, long offeringId, String academicYear,
            Map<String, DisciplineDTO> disciplineByName, String authHeader) {

        Map<String, Object> hours = buildHours(academicYear, detail);
        DisciplineDTO existing = disciplineByName.get(normKey(detail.getName()));
        if (existing != null) {
            SpecialtyDisciplineDTO sd = gradeClient.findSpecialtyDiscipline(offeringId, existing.getId(), authHeader);
            if (sd != null) return sd;
            return gradeClient.createSpecialtyDisciplineWithHours(offeringId, existing.getId(), hours, authHeader);
        }

        DisciplineCreateResponseDTO created = gradeClient.createDisciplineWithSD(
                detail.getName(), offeringId, hours, authHeader);
        DisciplineDTO newDisc = new DisciplineDTO();
        newDisc.setId(created.getDisciplineId());
        newDisc.setName(detail.getName());
        disciplineByName.put(normKey(detail.getName()), newDisc);
        SpecialtyDisciplineDTO sd = new SpecialtyDisciplineDTO();
        sd.setId(created.getSpecialtyDisciplineId());
        return sd;
    }

    private static Map<String, Object> buildHours(String academicYear, JournalDisciplineDetailDTO detail) {
        int ects = detail.getEctsCredits() != null ? detail.getEctsCredits() : 1;
        int total = detail.getTotalHours() != null ? detail.getTotalHours() : ects * 30;
        Map<String, Object> h = new HashMap<>();
        h.put("academicYear", academicYear);
        h.put("ectsCredits", ects);
        h.put("totalHours", total);
        h.put("classroomHours", 0);
        return h;
    }

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
