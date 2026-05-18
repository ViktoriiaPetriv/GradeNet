package org.bachelor.integrationservice.controller;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.bachelor.integrationservice.model.dto.*;
import org.bachelor.integrationservice.service.ExcelParserService;
import org.bachelor.integrationservice.service.ExcelValidationService;
import org.bachelor.integrationservice.service.ImportService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/import")
@RequiredArgsConstructor
public class ImportController {

    private final ImportService importService;
    private final ExcelParserService excelParserService;
    private final ExcelValidationService excelValidationService;
    private final ObjectMapper objectMapper;

    @PostMapping(value = "/check-disciplines", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public DisciplineCheckResultDTO checkDisciplines(
            @RequestPart("file") MultipartFile file,
            @RequestHeader("Authorization") String authHeader) throws IOException {
        excelValidationService.validate(file);
        return importService.checkDisciplines(file.getInputStream(), authHeader);
    }

    @PostMapping(value = "/create-disciplines", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public List<CreatedDisciplineInfoDTO> createDisciplines(
            @RequestPart("file") MultipartFile file,
            @RequestParam String disciplineIndices,
            @RequestParam Long specialtyOfferingId,
            @RequestParam String academicYear,
            @RequestHeader("Authorization") String authHeader) throws IOException {
        excelValidationService.validate(file);
        List<Integer> indices = objectMapper.readValue(disciplineIndices, new TypeReference<>() {});
        return importService.createDisciplines(file.getInputStream(), indices, specialtyOfferingId, academicYear, authHeader);
    }

    @PostMapping(value = "/check-students", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public StudentCheckResultDTO checkStudents(
            @RequestPart("file") MultipartFile file,
            @RequestHeader("Authorization") String authHeader) throws IOException {
        excelValidationService.validate(file);
        return importService.checkStudents(file.getInputStream(), authHeader);
    }

    @PostMapping(value = "/parse", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ParsedReportMetaDTO parse(@RequestPart("file") MultipartFile file) throws IOException {
        excelValidationService.validate(file);
        var report = excelParserService.parse(file.getInputStream());
        return new ParsedReportMetaDTO(report.getGroupName(), report.getAcademicYear(),
                report.getSpecialtyName(), report.getDisciplineNames());
    }

    /**
     * professorMap — JSON string: {"0": 5, "2": 3, ...}
     * Key = discipline index (0-based), value = professorId.
     * Disciplines without an entry in the map are skipped.
     *
     * selectedStudentBookNumberIds (optional) — JSON string: [42, 7, 15]
     * If provided, only students with these bookNumberIds will have grades imported.
     */
    @PostMapping(value = "/grade-report", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ImportResultDTO importGradeReport(
            @RequestPart("file") MultipartFile file,
            @RequestParam String professorMap,
            @RequestParam(required = false) String selectedStudentBookNumberIds,
            @RequestHeader("Authorization") String authHeader) throws IOException {

        excelValidationService.validate(file);
        Map<Integer, Long> professorByDiscipline = objectMapper.readValue(
                professorMap, new TypeReference<>() {});

        List<Long> selectedBookNumbers = null;
        if (selectedStudentBookNumberIds != null && !selectedStudentBookNumberIds.isBlank()) {
            selectedBookNumbers = objectMapper.readValue(selectedStudentBookNumberIds, new TypeReference<>() {});
        }

        return importService.importGradeReport(file, professorByDiscipline, selectedBookNumbers, authHeader);
    }
}
