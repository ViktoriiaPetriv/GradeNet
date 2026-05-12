package org.bachelor.integrationservice.service;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.bachelor.integrationservice.model.ParsedDiscipline;
import org.bachelor.integrationservice.model.ParsedGrade;
import org.bachelor.integrationservice.model.ParsedReport;
import org.bachelor.integrationservice.model.ParsedStudentRow;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ExcelParserService {

    private static final int DISCIPLINE_START_COL = 2;
    private static final int DISCIPLINE_COL_STEP = 3;
    private static final int DISCIPLINE_ROW = 10;  // 0-indexed = row 11
    private static final int GROUP_ROW = 6;         // 0-indexed = row 7
    private static final int YEAR_ROW = 7;          // 0-indexed = row 8
    private static final int STUDENT_START_ROW = 12; // 0-indexed = row 13
    private static final int SUMMARY_COL_START = 65;

    public ParsedReport parse(InputStream inputStream) throws IOException {
        try (Workbook workbook = new XSSFWorkbook(inputStream)) {
            Sheet sheet = workbook.getSheetAt(0);

            Row groupRow = sheet.getRow(GROUP_ROW);
            String groupName = parseGroupName(groupRow);
            String specialtyName = parseSpecialtyName(groupRow);
            String academicYear = parseAcademicYear(sheet);
            List<ParsedDiscipline> disciplines = parseDisciplines(sheet.getRow(DISCIPLINE_ROW));
            List<ParsedStudentRow> students = parseStudents(sheet, disciplines.size());

            return new ParsedReport(groupName, academicYear, specialtyName, disciplines, students);
        }
    }

    private String parseGroupName(Row row) {
        if (row == null) return null;
        String text = getCellString(row.getCell(0));
        if (text == null) return null;
        Matcher m = Pattern.compile("групи\\s+(\\S+)").matcher(text);
        return m.find() ? m.group(1) : null;
    }

    // "успішності студентів 3 курсу групи ІПЗ-32 спеціальності Інженерія програмного забезпечення"
    private String parseSpecialtyName(Row row) {
        if (row == null) return null;
        String text = getCellString(row.getCell(0));
        if (text == null) return null;
        Matcher m = Pattern.compile("спеціальності\\s+(.+)$").matcher(text.trim());
        return m.find() ? m.group(1).trim() : null;
    }

    private static final org.slf4j.Logger log =
            org.slf4j.LoggerFactory.getLogger(ExcelParserService.class);

    // Matches YYYY-YYYY, YYYY - YYYY, YYYY/YYYY, YYYY–YYYY, YYYY—YYYY
    private static final Pattern YEAR_PATTERN =
            Pattern.compile("(\\d{4})\\s*[\\-/–—]\\s*(\\d{4})");

    private String parseAcademicYear(Sheet sheet) {
        for (int rowIdx = 0; rowIdx <= Math.min(DISCIPLINE_ROW, sheet.getLastRowNum()); rowIdx++) {
            Row row = sheet.getRow(rowIdx);
            if (row == null) continue;
            for (int col = 0; col < row.getLastCellNum(); col++) {
                String text = getCellString(row.getCell(col));
                if (text == null) continue;
                log.info("Row {} col {}: {}", rowIdx, col, text);
                Matcher m = YEAR_PATTERN.matcher(text);
                if (m.find()) {
                    String year = m.group(1) + "/" + m.group(2);
                    log.info("Parsed academic year: {} (from row {} col {})", year, rowIdx, col);
                    return year;
                }
            }
        }
        log.warn("Could not parse academic year from sheet");
        return null;
    }

    private static final java.util.regex.Pattern HOURS_PATTERN =
            java.util.regex.Pattern.compile("(\\d+)\\s*год\\.");
    private static final java.util.regex.Pattern SEMESTER_PATTERN =
            java.util.regex.Pattern.compile("(\\d+)\\s*сем\\.");

    private List<ParsedDiscipline> parseDisciplines(Row row) {
        List<ParsedDiscipline> result = new ArrayList<>();
        if (row == null) return result;
        for (int col = DISCIPLINE_START_COL; col < SUMMARY_COL_START; col += DISCIPLINE_COL_STEP) {
            String raw = getCellString(row.getCell(col));
            if (raw == null || raw.isBlank()) break;
            // "Bussines English. (дисц.), 90 год., 6сем." → name="Bussines English.", totalHours=90, semester=6
            String name = raw.replaceAll("\\s*\\(дисц\\.\\).*$", "").trim();
            java.util.regex.Matcher hoursMatcher = HOURS_PATTERN.matcher(raw);
            int totalHours = hoursMatcher.find() ? Integer.parseInt(hoursMatcher.group(1)) : 90;
            java.util.regex.Matcher semMatcher = SEMESTER_PATTERN.matcher(raw);
            Integer semester = semMatcher.find() ? Integer.parseInt(semMatcher.group(1)) : null;
            result.add(ParsedDiscipline.of(name, totalHours, semester));
        }
        return result;
    }

    private List<ParsedStudentRow> parseStudents(Sheet sheet, int disciplineCount) {
        List<ParsedStudentRow> students = new ArrayList<>();
        for (int rowIdx = STUDENT_START_ROW; rowIdx <= sheet.getLastRowNum(); rowIdx++) {
            Row row = sheet.getRow(rowIdx);
            if (row == null) break;

            String rawName = getCellString(row.getCell(1));
            if (rawName == null || rawName.isBlank()) break;

            // Normalize all Unicode whitespace (incl. U+00A0 non-breaking space) to regular space
            String name = rawName.replaceAll("[\\p{Z}\\s]+", " ").trim();
            // Remove /к/ or /б/ suffix and everything after the first slash
            int slashIdx = name.indexOf('/');
            if (slashIdx >= 0) name = name.substring(0, slashIdx).trim();
            if (name.isBlank()) break;

            List<ParsedGrade> grades = new ArrayList<>();
            for (int d = 0; d < disciplineCount; d++) {
                int startCol = DISCIPLINE_START_COL + d * DISCIPLINE_COL_STEP;
                Integer score = getCellInt(row.getCell(startCol));
                String ects = getCellString(row.getCell(startCol + 1));
                Object national = getCellValue(row.getCell(startCol + 2));

                if (score != null || ects != null) {
                    grades.add(new ParsedGrade(d, score, ects, national));
                }
            }

            students.add(new ParsedStudentRow(name, grades));
        }
        return students;
    }

    private String getCellString(Cell cell) {
        if (cell == null) return null;
        return switch (cell.getCellType()) {
            case STRING -> {
                String s = cell.getStringCellValue().replaceAll("[\\p{Z}\\s]+", " ").trim();
                yield s.isEmpty() ? null : s;
            }
            case NUMERIC -> String.valueOf((int) cell.getNumericCellValue());
            default -> null;
        };
    }

    private Integer getCellInt(Cell cell) {
        if (cell == null) return null;
        return switch (cell.getCellType()) {
            case NUMERIC -> (int) cell.getNumericCellValue();
            case STRING -> {
                try { yield Integer.parseInt(cell.getStringCellValue().trim()); }
                catch (NumberFormatException e) { yield null; }
            }
            default -> null;
        };
    }

    private Object getCellValue(Cell cell) {
        if (cell == null) return null;
        return switch (cell.getCellType()) {
            case STRING -> {
                String s = cell.getStringCellValue().trim();
                yield s.isEmpty() ? null : s;
            }
            case NUMERIC -> (int) cell.getNumericCellValue();
            default -> null;
        };
    }
}
