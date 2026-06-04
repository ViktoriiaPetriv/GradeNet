package org.bachelor.integrationservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bachelor.integrationservice.mapper.GradeMapper;
import org.bachelor.integrationservice.model.ParsedDiscipline;
import org.bachelor.integrationservice.model.ParsedGrade;
import org.bachelor.integrationservice.model.ParsedReport;
import org.bachelor.integrationservice.model.ParsedStudentRow;
import org.bachelor.integrationservice.model.client.*;
import org.bachelor.integrationservice.model.dto.*;
import org.bachelor.integrationservice.service.client.GradeServiceClient;
import org.bachelor.integrationservice.service.client.OrgServiceClient;
import org.bachelor.integrationservice.service.client.UserServiceClient;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class ImportService {

    private final ExcelParserService excelParserService;
    private final GradeMapper gradeMapper;
    private final GradeServiceClient gradeClient;
    private final UserServiceClient userClient;
    private final OrgServiceClient orgClient;

    public DisciplineCheckResultDTO checkDisciplines(InputStream fileInputStream, Long specialtyId, String authHeader) throws IOException {
        ParsedReport report = excelParserService.parse(fileInputStream);
        String academicYear = nonBlank(report.getAcademicYear(), defaultAcademicYear());

        SpecialtyDTO specialty = specialtyId != null
                ? orgClient.getSpecialtyById(specialtyId, authHeader)
                : orgClient.resolveSpecialtyByName(report.getSpecialtyName(), authHeader);
        log.info("checkDisciplines: specialtyId={}, specialtyName='{}', specialty={}, firstSemester={}",
                specialtyId, report.getSpecialtyName(),
                specialty != null ? specialty.getId() + "/" + specialty.getDegree() : "NOT FOUND",
                getFirstSemester(report));

        Long resolvedSpecialtyId = specialty != null ? specialty.getId() : null;
        Integer graduationYear = specialty != null
                ? ExcelParserService.calculateGraduationYear(academicYear, getFirstSemester(report), specialty.getDegree())
                : null;
        Long specialtyOfferingId = (resolvedSpecialtyId != null && graduationYear != null)
                ? orgClient.resolveOfferingIdBySpecialtyId(resolvedSpecialtyId, graduationYear, authHeader)
                : null;
        if (specialtyOfferingId == null) {
            log.warn("Could not resolve specialty offering for '{}' graduation year {}", report.getSpecialtyName(), graduationYear);
        }

        Map<String, DisciplineDTO> disciplineByName = buildDisciplineNameMap(authHeader);

        List<DisciplineCheckItemDTO> disciplineItems = new ArrayList<>();
        for (ParsedDiscipline parsed : report.getDisciplines()) {
            String effectiveName = stripRetakeSuffix(parsed.getName());
            DisciplineDTO existing = disciplineByName.get(normalizeDisciplineKey(effectiveName));
            Long disciplineId = existing != null ? existing.getId() : null;
            Long specialtyDisciplineId = null;
            if (existing != null && specialtyOfferingId != null) {
                SpecialtyDisciplineDTO sd = gradeClient.findSpecialtyDiscipline(specialtyOfferingId, disciplineId, authHeader);
                specialtyDisciplineId = sd != null ? sd.getId() : null;
            }
            disciplineItems.add(DisciplineCheckItemDTO.builder()
                    .index(report.getDisciplines().indexOf(parsed))
                    .name(parsed.getName())
                    .totalHours(parsed.getTotalHours())
                    .ectsCredits(parsed.getEctsCredits())
                    .existsInSystem(existing != null)
                    .disciplineId(disciplineId)
                    .specialtyDisciplineId(specialtyDisciplineId)
                    .semester(parsed.getSemester())
                    .build());
        }

        return DisciplineCheckResultDTO.builder()
                .groupName(report.getGroupName())
                .academicYear(academicYear)
                .specialtyName(report.getSpecialtyName())
                .specialtyId(resolvedSpecialtyId)
                .graduationYear(graduationYear)
                .specialtyOfferingId(specialtyOfferingId)
                .disciplines(disciplineItems)
                .build();
    }

    public List<CreatedDisciplineInfoDTO> createDisciplines(
            InputStream fileInputStream, List<Integer> indices, Long specialtyOfferingId,
            String academicYear, String authHeader) throws IOException {
        ParsedReport report = excelParserService.parse(fileInputStream);
        String year = nonBlank(academicYear, defaultAcademicYear());

        Map<String, Long> disciplineIdByName = new HashMap<>();
        for (DisciplineDTO d : gradeClient.getAllDisciplines(authHeader)) {
            disciplineIdByName.put(normalizeDisciplineKey(d.getName()), d.getId());
        }

        Map<Long, Long> sdByDisciplineId = new HashMap<>();
        List<CreatedDisciplineInfoDTO> result = new ArrayList<>();

        for (int idx : indices) {
            if (idx < 0 || idx >= report.getDisciplines().size()) continue;
            ParsedDiscipline parsed = report.getDisciplines().get(idx);
            String effectiveName = stripRetakeSuffix(parsed.getName());
            try {
                Long disciplineId = disciplineIdByName.get(normalizeDisciplineKey(effectiveName));
                long sdId;
                if (disciplineId != null) {
                    Long existingSdId = sdByDisciplineId.get(disciplineId);
                    if (existingSdId == null) {
                        SpecialtyDisciplineDTO existing = gradeClient.findSpecialtyDiscipline(specialtyOfferingId, disciplineId, authHeader);
                        existingSdId = existing != null ? existing.getId()
                                : gradeClient.createSpecialtyDisciplineWithHours(specialtyOfferingId, disciplineId, year, parsed, authHeader).getId();
                        sdByDisciplineId.put(disciplineId, existingSdId);
                    }
                    sdId = existingSdId;
                    log.info("Reused discipline at index {}: {} (id={})", idx, effectiveName, disciplineId);
                } else {
                    DisciplineCreateResponseDTO created = gradeClient.createDisciplineWithSD(effectiveName, specialtyOfferingId, year, parsed, authHeader);
                    disciplineId = created.getDisciplineId();
                    sdId = created.getSpecialtyDisciplineId();
                    disciplineIdByName.put(normalizeDisciplineKey(effectiveName), disciplineId);
                    sdByDisciplineId.put(disciplineId, sdId);
                    log.info("Created discipline at index {}: {} (id={})", idx, effectiveName, disciplineId);
                }
                result.add(CreatedDisciplineInfoDTO.builder()
                        .index(idx)
                        .disciplineId(disciplineId)
                        .specialtyDisciplineId(sdId)
                        .build());
            } catch (Exception e) {
                log.error("Failed to create discipline at index {}: {}", idx, e.getMessage());
                throw new RuntimeException("Failed to create discipline: " + effectiveName, e);
            }
        }
        return result;
    }

    public StudentCheckResultDTO checkStudents(InputStream fileInputStream, String authHeader) throws IOException {
        ParsedReport report = excelParserService.parse(fileInputStream);

        GroupDTO group = userClient.findGroup(report.getGroupName(), authHeader);
        if (group == null) {
            log.warn("Group not found: {}", report.getGroupName());
            return StudentCheckResultDTO.builder()
                    .groupName(report.getGroupName())
                    .students(new ArrayList<>())
                    .build();
        }

        List<GroupMemberDTO> members = new ArrayList<>(userClient.getGroupMembers(group.getId(), authHeader));
        if (members.isEmpty()) {
            log.info("Group {} has no members, searching system students by name", group.getName());
            members = findAndAddGroupMembers(group.getId(), report.getStudents(), authHeader, new ArrayList<>());
        }

        String academicYear = nonBlank(report.getAcademicYear(), defaultAcademicYear());
        SpecialtyDTO specialty = orgClient.resolveSpecialtyByName(report.getSpecialtyName(), authHeader);
        Integer reportGradYear = ExcelParserService.calculateGraduationYear(
                academicYear, getFirstSemester(report), specialty != null ? specialty.getDegree() : null);
        Long reportOfferingId = specialty != null
                ? orgClient.resolveOfferingIdBySpecialtyId(specialty.getId(), reportGradYear, authHeader)
                : null;

        List<StudentCheckItemDTO> studentItems = new ArrayList<>();
        for (ParsedStudentRow studentRow : report.getStudents()) {
            GroupMemberDTO member = matchStudent(studentRow.getFullName(), members);

            boolean graduationYearMismatch = false;
            if (member != null && reportOfferingId != null) {
                Long studentOfferingId = userClient.resolveSpecialtyOfferingId(member.getBookNumberId(), authHeader);
                graduationYearMismatch = !reportOfferingId.equals(studentOfferingId);
                if (graduationYearMismatch) {
                    Integer studentGradYear = orgClient.getOfferingGraduationYear(studentOfferingId, authHeader);
                    log.warn("Graduation year mismatch for {}: student={}, report={}",
                            studentRow.getFullName(), studentGradYear, reportGradYear);
                }
            }

            List<GradeDataDTO> gradeData = studentRow.getGrades().stream()
                    .map(gradeMapper::toGradeDataDTO)
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

    public ImportResultDTO importGradeReport(MultipartFile file,
                                             Map<Integer, Long> professorByDiscipline,
                                             List<Long> selectedStudentBookNumberIds,
                                             boolean overwrite,
                                             String authHeader) throws IOException {
        ParsedReport report = excelParserService.parse(file.getInputStream());
        String academicYear = nonBlank(report.getAcademicYear(), defaultAcademicYear());

        List<ImportErrorDTO> errors = new ArrayList<>();
        List<String> unmatchedStudents = new ArrayList<>();

        GroupDTO group = userClient.findGroup(report.getGroupName(), authHeader);
        if (group == null) {
            errors.add(new ImportErrorDTO(null, null, "Групу не знайдено: " + report.getGroupName()));
            return buildResult(report, 0, 0, 0, unmatchedStudents, errors);
        }

        List<GroupMemberDTO> members = new ArrayList<>(userClient.getGroupMembers(group.getId(), authHeader));
        if (members.isEmpty()) {
            log.info("Group {} has no members, searching system students by name", group.getName());
            members = findAndAddGroupMembers(group.getId(), report.getStudents(), authHeader, errors);
            if (members.isEmpty()) {
                errors.add(new ImportErrorDTO(null, null,
                        "Не вдалося знайти студентів у системі для групи: " + group.getName()));
                return buildResult(report, 0, 0, 0, unmatchedStudents, errors);
            }
        }

        Long targetOfferingId = orgClient.resolveOfferingBySpecialtyName(
                report.getSpecialtyName(), academicYear, getFirstSemester(report), authHeader);

        Map<Long, Long> memberOfferingMap = new HashMap<>();
        for (GroupMemberDTO m : members) {
            Long sid = userClient.resolveSpecialtyOfferingId(m.getBookNumberId(), authHeader);
            if (sid != null) memberOfferingMap.put(m.getBookNumberId(), sid);
        }

        Long specialtyOfferingId = targetOfferingId != null
                ? targetOfferingId
                : memberOfferingMap.get(members.get(0).getBookNumberId());
        if (specialtyOfferingId == null) {
            errors.add(new ImportErrorDTO(null, null, "Не вдалося визначити спеціальність із залікової книжки студента"));
            return buildResult(report, 0, 0, 0, unmatchedStudents, errors);
        }

        final Integer reportGradYear = orgClient.getOfferingGraduationYear(specialtyOfferingId, authHeader);

        List<SpecialtyDisciplineDTO> specialtyDisciplines = resolveSpecialtyDisciplines(
                report.getDisciplines(), specialtyOfferingId, academicYear,
                professorByDiscipline.keySet(), authHeader, errors);

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
                Integer studentGradYear = orgClient.getOfferingGraduationYear(studentOfferingId, authHeader);
                String msg = studentGradYear != null && reportGradYear != null
                        ? String.format("Студент належить до випуску %d, але звіт для випуску %d", studentGradYear, reportGradYear)
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
                if (professorId == null) continue;
                if (idx >= specialtyDisciplines.size()) continue;
                SpecialtyDisciplineDTO sd = specialtyDisciplines.get(idx);
                if (sd == null) continue;

                String disciplineName = report.getDisciplineNames().get(idx);
                boolean isRetake = !stripRetakeSuffix(disciplineName).equals(disciplineName);
                try {
                    Integer disciplineSemester = idx < report.getDisciplines().size()
                            ? report.getDisciplines().get(idx).getSemester() : null;
                    GradeBookEntryDTO entry = isRetake
                            ? gradeClient.findOrCreateRetakeEntry(member.getBookNumberId(), sd.getId(), professorId, academicYear, disciplineSemester, authHeader)
                            : gradeClient.findOrCreateEntry(member.getBookNumberId(), sd.getId(), professorId, academicYear, disciplineSemester, overwrite, authHeader);
                    gradeClient.createGrade(entry.getId(), grade, authHeader);
                    entryIdsToClose.add(entry.getId());
                    gradesCreated++;
                } catch (Exception e) {
                    log.error("Failed to create grade for {} / {}: {}", studentRow.getFullName(), disciplineName, e.getMessage());
                    errors.add(new ImportErrorDTO(studentRow.getFullName(), disciplineName, e.getMessage()));
                }
            }

            if (!entryIdsToClose.isEmpty()) {
                try {
                    gradeClient.closeEntries(entryIdsToClose, authHeader);
                } catch (Exception e) {
                    log.warn("Failed to close entries for student {}: {}", studentRow.getFullName(), e.getMessage());
                }
            }
        }

        long disciplinesWithProfessor = professorByDiscipline.values().stream()
                .filter(Objects::nonNull).count();
        return buildResult(report, (int) disciplinesWithProfessor, studentsMatched, gradesCreated, unmatchedStudents, errors);
    }

    public GradeComparisonResultDTO compareGrades(MultipartFile file,
                                                   Map<Integer, Long> professorByDiscipline,
                                                   List<Long> selectedStudentBookNumberIds,
                                                   String authHeader) throws IOException {
        ParsedReport report = excelParserService.parse(file.getInputStream());
        String academicYear = nonBlank(report.getAcademicYear(), defaultAcademicYear());

        GroupDTO group = userClient.findGroup(report.getGroupName(), authHeader);
        if (group == null) {
            return GradeComparisonResultDTO.builder()
                    .groupName(report.getGroupName())
                    .academicYear(academicYear)
                    .disciplines(List.of())
                    .students(List.of())
                    .hasAnyDiff(false)
                    .build();
        }

        List<GroupMemberDTO> members = new ArrayList<>(userClient.getGroupMembers(group.getId(), authHeader));
        if (members.isEmpty()) {
            members = findAndAddGroupMembers(group.getId(), report.getStudents(), authHeader, new ArrayList<>());
        }

        Long specialtyOfferingId = orgClient.resolveOfferingBySpecialtyName(
                report.getSpecialtyName(), academicYear, getFirstSemester(report), authHeader);
        if (specialtyOfferingId == null && !members.isEmpty()) {
            specialtyOfferingId = userClient.resolveSpecialtyOfferingId(members.get(0).getBookNumberId(), authHeader);
        }

        Map<Integer, SpecialtyDisciplineDTO> sdByIndex = resolveSpecialtyDisciplinesReadOnly(
                report.getDisciplines(), specialtyOfferingId, professorByDiscipline.keySet(), authHeader);

        Map<Integer, Map<Long, BulkEntryClientDTO>> existingByDiscipline = new HashMap<>();
        for (int idx : professorByDiscipline.keySet()) {
            SpecialtyDisciplineDTO sd = sdByIndex.get(idx);
            if (sd == null) continue;
            List<BulkEntryClientDTO> entries = gradeClient.getGroupReportForDiscipline(sd.getId(), academicYear, authHeader);
            Map<Long, BulkEntryClientDTO> byBookNumber = new HashMap<>();
            for (BulkEntryClientDTO e : entries) byBookNumber.put(e.getBookNumberId(), e);
            existingByDiscipline.put(idx, byBookNumber);
        }

        List<GradeComparisonStudentDTO> comparisonStudents = new ArrayList<>();
        for (ParsedStudentRow studentRow : report.getStudents()) {
            GroupMemberDTO member = matchStudent(studentRow.getFullName(), members);
            if (member == null) continue;
            if (selectedStudentBookNumberIds != null
                    && !selectedStudentBookNumberIds.contains(member.getBookNumberId())) continue;

            List<GradeComparisonCellDTO> cells = new ArrayList<>();
            for (ParsedGrade grade : studentRow.getGrades()) {
                int idx = grade.getDisciplineIndex();
                if (!professorByDiscipline.containsKey(idx)) continue;
                if (!sdByIndex.containsKey(idx)) continue;

                Map<Long, BulkEntryClientDTO> disciplineMap = existingByDiscipline.get(idx);
                BulkEntryClientDTO existing = disciplineMap != null ? disciplineMap.get(member.getBookNumberId()) : null;

                Integer existingGrade = null;
                Long existingEntryId = null;
                boolean isNew = (existing == null);
                if (existing != null) {
                    existingEntryId = existing.getEntryId();
                    if (existing.getLatestGrade() != null) existingGrade = existing.getLatestGrade().getUniversityGrade();
                }
                boolean hasDiff = !isNew && !Objects.equals(grade.getUniversityGrade(), existingGrade);

                cells.add(GradeComparisonCellDTO.builder()
                        .disciplineIndex(idx)
                        .fileGrade(grade.getUniversityGrade())
                        .existingGrade(existingGrade)
                        .existingEntryId(existingEntryId)
                        .hasDiff(hasDiff)
                        .isNew(isNew)
                        .build());
            }

            comparisonStudents.add(GradeComparisonStudentDTO.builder()
                    .fullName(studentRow.getFullName())
                    .bookNumberId(member.getBookNumberId())
                    .cells(cells)
                    .build());
        }

        boolean hasAnyDiff = comparisonStudents.stream()
                .flatMap(s -> s.getCells().stream())
                .anyMatch(GradeComparisonCellDTO::isHasDiff);

        List<GradeComparisonDisciplineDTO> disciplines = professorByDiscipline.keySet().stream()
                .sorted()
                .filter(sdByIndex::containsKey)
                .map(idx -> gradeMapper.toDisciplineDTO(idx,
                        idx < report.getDisciplines().size() ? report.getDisciplines().get(idx).getName() : ""))
                .toList();

        return GradeComparisonResultDTO.builder()
                .groupName(report.getGroupName())
                .academicYear(academicYear)
                .disciplines(disciplines)
                .students(comparisonStudents)
                .hasAnyDiff(hasAnyDiff)
                .build();
    }

    private List<SpecialtyDisciplineDTO> resolveSpecialtyDisciplines(
            List<ParsedDiscipline> disciplines, Long specialtyOfferingId, String academicYear,
            Set<Integer> selectedIndices, String authHeader, List<ImportErrorDTO> errors) {

        Map<String, DisciplineDTO> disciplineByName = buildDisciplineNameMap(authHeader);
        List<SpecialtyDisciplineDTO> result = new ArrayList<>();

        for (int idx = 0; idx < disciplines.size(); idx++) {
            if (!selectedIndices.contains(idx)) {
                result.add(null);
                continue;
            }
            ParsedDiscipline parsed = disciplines.get(idx);
            try {
                String effectiveName = stripRetakeSuffix(parsed.getName());
                if (!effectiveName.equals(parsed.getName())) {
                    log.info("Retake discipline detected: '{}' → using base '{}'", parsed.getName(), effectiveName);
                }
                DisciplineDTO discipline = disciplineByName.get(normalizeDisciplineKey(effectiveName));
                SpecialtyDisciplineDTO sd;
                if (discipline == null) {
                    DisciplineCreateResponseDTO created = gradeClient.createDisciplineWithSD(
                            effectiveName, specialtyOfferingId, academicYear, parsed, authHeader);
                    DisciplineDTO newDisc = new DisciplineDTO();
                    newDisc.setId(created.getDisciplineId());
                    newDisc.setName(created.getName());
                    disciplineByName.put(normalizeDisciplineKey(effectiveName), newDisc);
                    sd = new SpecialtyDisciplineDTO();
                    sd.setId(created.getSpecialtyDisciplineId());
                } else {
                    sd = gradeClient.findSpecialtyDiscipline(specialtyOfferingId, discipline.getId(), authHeader);
                    if (sd == null) {
                        sd = gradeClient.createSpecialtyDisciplineWithHours(
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

    private Map<Integer, SpecialtyDisciplineDTO> resolveSpecialtyDisciplinesReadOnly(
            List<ParsedDiscipline> disciplines, Long specialtyOfferingId,
            Set<Integer> selectedIndices, String authHeader) {
        if (specialtyOfferingId == null) return Map.of();
        Map<String, DisciplineDTO> disciplineByName = buildDisciplineNameMap(authHeader);
        Map<Integer, SpecialtyDisciplineDTO> result = new HashMap<>();
        for (int idx : selectedIndices) {
            if (idx >= disciplines.size()) continue;
            String effectiveName = stripRetakeSuffix(disciplines.get(idx).getName());
            DisciplineDTO discipline = disciplineByName.get(normalizeDisciplineKey(effectiveName));
            if (discipline == null) continue;
            SpecialtyDisciplineDTO sd = gradeClient.findSpecialtyDiscipline(specialtyOfferingId, discipline.getId(), authHeader);
            if (sd != null) result.put(idx, sd);
        }
        return result;
    }

    private List<GroupMemberDTO> findAndAddGroupMembers(Long groupId,
                                                        List<ParsedStudentRow> students,
                                                        String authHeader,
                                                        List<ImportErrorDTO> errors) {
        List<UserDTO> allStudents = userClient.getAllStudents(authHeader);
        List<GroupMemberDTO> result = new ArrayList<>();

        for (ParsedStudentRow studentRow : students) {
            String[] parts = normalizeName(studentRow.getFullName()).split("\\s+");
            if (parts.length == 0 || parts[0].isBlank()) continue;
            String lastName = parts[0].toLowerCase();
            String firstInitial = parts.length > 1 ? parts[1].replace(".", "").toLowerCase() : null;

            UserDTO matched = allStudents.stream()
                    .filter(u -> u.getLastName() != null &&
                            normalizeName(u.getLastName()).toLowerCase().equals(lastName))
                    .filter(u -> firstInitial == null || u.getFirstName() == null ||
                            normalizeName(u.getFirstName()).substring(0, 1).toLowerCase().equals(firstInitial))
                    .findFirst().orElse(null);

            if (matched == null) {
                log.warn("Student not found in system: {}", studentRow.getFullName());
                errors.add(new ImportErrorDTO(studentRow.getFullName(), null, "Студента не знайдено в системі"));
                continue;
            }

            List<BookNumberDTO> books = userClient.getStudentBooks(matched.getId(), authHeader);
            BookNumberDTO activeBook = books.stream()
                    .filter(b -> "REGISTERED".equals(b.getStatus()) || "FILLED".equals(b.getStatus()))
                    .findFirst().orElse(null);

            if (activeBook == null) {
                log.warn("No active book number for student: {}", studentRow.getFullName());
                errors.add(new ImportErrorDTO(studentRow.getFullName(), null, "Немає активної залікової книжки"));
                continue;
            }

            try {
                GroupMemberDTO member = userClient.addMemberToGroup(groupId, activeBook.getId(), authHeader);
                if (member != null) result.add(member);
            } catch (Exception e) {
                log.warn("Could not add {} to group (may already be a member): {}",
                        studentRow.getFullName(), e.getMessage());
                GroupMemberDTO synthetic = new GroupMemberDTO();
                synthetic.setBookNumberId(activeBook.getId());
                synthetic.setStudentId(matched.getId());
                synthetic.setStudentName(matched.getLastName() + " " + matched.getFirstName());
                result.add(synthetic);
            }
        }
        return result;
    }

    private Map<String, DisciplineDTO> buildDisciplineNameMap(String authHeader) {
        Map<String, DisciplineDTO> map = new HashMap<>();
        for (DisciplineDTO d : gradeClient.getAllDisciplines(authHeader)) {
            map.put(normalizeDisciplineKey(d.getName()), d);
        }
        return map;
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

    private static String stripRetakeSuffix(String name) {
        return name.replaceAll("(?iU)[,.]?\\s*\\(?повторн[еий]+\\s+(?:курс|вивчення)\\)?\\s*$", "").trim();
    }

    private static String normalizeDisciplineKey(String name) {
        if (name == null) return null;
        return name.trim().toLowerCase()
                .replace('а', 'a').replace('е', 'e').replace('о', 'o')
                .replace('с', 'c').replace('р', 'p').replace('х', 'x')
                .replace('і', 'i');
    }

    private static String normalizeName(String s) {
        return s.replaceAll("[\\p{Z}\\s]+", " ")
                .replace("`", "'").replace("‘", "'")
                .replace("’", "'").replace("ʼ", "'")
                .trim();
    }
    private static String nonBlank(String s, String fallback) {
        return (s != null && !s.isBlank()) ? s : fallback;
    }

    private static String defaultAcademicYear() {
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
