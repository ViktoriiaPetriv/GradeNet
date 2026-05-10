package org.bachelor.integrationservice.controller;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.bachelor.integrationservice.model.dto.ImportResultDTO;
import org.bachelor.integrationservice.model.dto.ParsedReportMetaDTO;
import org.bachelor.integrationservice.service.ExcelParserService;
import org.bachelor.integrationservice.service.ExcelValidationService;
import org.bachelor.integrationservice.service.ImportService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api/import")
@RequiredArgsConstructor
public class ImportController {

    private final ImportService importService;
    private final ExcelParserService excelParserService;
    private final ExcelValidationService excelValidationService;
    private final ObjectMapper objectMapper;

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
     */
    @PostMapping(value = "/grade-report", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ImportResultDTO importGradeReport(
            @RequestPart("file") MultipartFile file,
            @RequestParam String professorMap,
            @RequestHeader("Authorization") String authHeader) throws IOException {

        excelValidationService.validate(file);
        Map<Integer, Long> professorByDiscipline = objectMapper.readValue(
                professorMap, new TypeReference<>() {});

        return importService.importGradeReport(file, professorByDiscipline, authHeader);
    }
}
