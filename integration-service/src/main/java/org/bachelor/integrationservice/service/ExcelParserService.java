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
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ExcelParserService {

    private static final int DISCIPLINE_START_COL = 2;
    private static final int DISCIPLINE_COL_STEP = 3;
    private static final int GROUP_ROW = 6;         // 0-indexed = row 7
    private static final int SUMMARY_COL_START = 65;

    // Scan rows 8-12 (0-indexed) for the first row where column C contains "(дисц.)"
    private int detectDisciplineRow(Sheet sheet) {
        for (int rowIdx = 8; rowIdx <= 12; rowIdx++) {
            Row row = sheet.getRow(rowIdx);
            if (row == null) continue;
            String cell = getCellString(row.getCell(DISCIPLINE_START_COL));
            if (cell != null && cell.contains("(дисц.)")) return rowIdx;
        }
        return 10; // default for zvit/zvit2
    }

    public ParsedReport parse(InputStream inputStream) throws IOException {
        try (Workbook workbook = new XSSFWorkbook(inputStream)) {
            Sheet sheet = workbook.getSheetAt(0);

            int disciplineRow = detectDisciplineRow(sheet);
            int studentStartRow = disciplineRow + 2;

            Row groupRow = sheet.getRow(GROUP_ROW);
            String groupName = parseGroupName(groupRow);
            String specialtyName = parseSpecialtyName(groupRow);
            String academicYear = parseAcademicYear(sheet, disciplineRow);
            List<ParsedDiscipline> disciplines = parseDisciplines(sheet.getRow(disciplineRow));
            List<ParsedStudentRow> students = parseStudents(sheet, disciplines.size(), studentStartRow);
            Integer graduationYear = academicYear != null
                    ? calculateGraduationYear(academicYear, disciplines)
                    : parseGraduationYearFromStudyPeriod(sheet, disciplineRow);

            return new ParsedReport(groupName, academicYear, graduationYear, specialtyName, disciplines, students);
        }
    }

    private String parseGroupName(Row row) {
        if (row == null) return null;
        String text = getCellString(row.getCell(0));
        if (text == null) return null;
        // Capture everything between "групи" and "спеціальності", then normalize spaces around dashes
        Matcher m = Pattern.compile("групи\\s+(.+?)\\s+спеціальності").matcher(text);
        if (!m.find()) return null;
        return m.group(1).replaceAll("\\s*[-–—]\\s*", "-").trim();
    }

    private String parseSpecialtyName(Row row) {
        if (row == null) return null;
        String text = getCellString(row.getCell(0));
        if (text == null) return null;
        // Try quoted specialty first: спеціальності "Назва"
        Matcher quoted = Pattern.compile("спеціальності\\s+\"(.+?)\"").matcher(text);
        if (quoted.find()) return quoted.group(1).trim();
        // Plain specialty: stop before " за " (year range follows) or end of string
        Matcher plain = Pattern.compile("спеціальності\\s+(.+?)(?:\\s+за\\s+|$)").matcher(text.trim());
        return plain.find() ? plain.group(1).trim() : null;
    }

    private static final org.slf4j.Logger log =
            org.slf4j.LoggerFactory.getLogger(ExcelParserService.class);

    // Matches YYYY-YYYY, YYYY - YYYY, YYYY/YYYY, YYYY–YYYY, YYYY—YYYY
    private static final Pattern YEAR_PATTERN =
            Pattern.compile("(\\d{4})\\s*[\\-/–—]\\s*(\\d{4})");

    private String parseAcademicYear(Sheet sheet, int disciplineRow) {
        for (int rowIdx = 0; rowIdx <= Math.min(disciplineRow, sheet.getLastRowNum()); rowIdx++) {
            Row row = sheet.getRow(rowIdx);
            if (row == null) continue;
            for (int col = 0; col < row.getLastCellNum(); col++) {
                String text = getCellString(row.getCell(col));
                if (text == null) continue;
                log.info("Row {} col {}: {}", rowIdx, col, text);
                Matcher m = YEAR_PATTERN.matcher(text);
                if (m.find()) {
                    int y1 = Integer.parseInt(m.group(1));
                    int y2 = Integer.parseInt(m.group(2));
                    if (y2 - y1 > 2) continue; // skip full study-period ranges (e.g. 2024-2028)
                    String year = y1 + "/" + y2;
                    log.info("Parsed academic year: {} (from row {} col {})", year, rowIdx, col);
                    return year;
                }
            }
        }
        log.warn("Could not parse academic year from sheet");
        return null;
    }

    // For cumulative reports (ЗВЕДЕНА ВІДОМІСТЬ) that contain a full study-period range
    // (e.g. "2024 - 2028"), extract the graduation year (end of range) directly.
    private Integer parseGraduationYearFromStudyPeriod(Sheet sheet, int disciplineRow) {
        for (int rowIdx = 0; rowIdx <= Math.min(disciplineRow, sheet.getLastRowNum()); rowIdx++) {
            Row row = sheet.getRow(rowIdx);
            if (row == null) continue;
            for (int col = 0; col < row.getLastCellNum(); col++) {
                String text = getCellString(row.getCell(col));
                if (text == null) continue;
                Matcher m = YEAR_PATTERN.matcher(text);
                if (m.find()) {
                    int y1 = Integer.parseInt(m.group(1));
                    int y2 = Integer.parseInt(m.group(2));
                    if (y2 - y1 > 2) {
                        log.info("Parsed graduation year {} from study period {}-{}", y2, y1, y2);
                        return y2;
                    }
                }
            }
        }
        return null;
    }

    // graduation year = end of academic year + remaining years in program
    // BACHELOR (8 semesters, 4 years): e.g. 2024/2025, sem 6 → 2025 + (4-3) = 2026
    // MASTER (3 semesters, treated as 2 academic years): e.g. 2024/2025, sem 2 → 2025 + (2-1) = 2026
    public static Integer calculateGraduationYear(String academicYear, Integer semester, String degree) {
        if (academicYear == null || semester == null) return null;
        Matcher yearMatcher = YEAR_PATTERN.matcher(academicYear);
        if (!yearMatcher.find()) return null;
        int endYear = Integer.parseInt(yearMatcher.group(2));
        int totalYears = "MASTER".equalsIgnoreCase(degree) ? 2 : 4;
        int currentYearInProgram = (semester + 1) / 2;
        int remainingYears = Math.max(0, totalYears - currentYearInProgram);
        return endYear + remainingYears;
    }

    private Integer calculateGraduationYear(String academicYear, List<ParsedDiscipline> disciplines) {
        Integer semester = disciplines.stream()
                .map(ParsedDiscipline::getSemester)
                .filter(Objects::nonNull)
                .findFirst().orElse(null);
        return calculateGraduationYear(academicYear, semester, null);
    }

    private static final Pattern HOURS_PATTERN = Pattern.compile("(\\d+)\\s*год\\.");
    private static final Pattern SEMESTER_PATTERN = Pattern.compile("(\\d+)\\s*сем\\.");

    private List<ParsedDiscipline> parseDisciplines(Row row) {
        List<ParsedDiscipline> result = new ArrayList<>();
        if (row == null) return result;
        for (int col = DISCIPLINE_START_COL; col < SUMMARY_COL_START; col += DISCIPLINE_COL_STEP) {
            String raw = getCellString(row.getCell(col));
            if (raw == null || raw.isBlank()) break;
            // "Bussines English. (дисц.), 90 год., 6сем." → name="Bussines English.", totalHours=90, semester=6
            String name = raw.replaceAll("\\s*\\(дисц\\.\\).*$", "").trim();
            Matcher hoursMatcher = HOURS_PATTERN.matcher(raw);
            int totalHours = hoursMatcher.find() ? Integer.parseInt(hoursMatcher.group(1)) : 90;
            Matcher semMatcher = SEMESTER_PATTERN.matcher(raw);
            Integer semester = semMatcher.find() ? Integer.parseInt(semMatcher.group(1)) : null;
            result.add(ParsedDiscipline.of(name, totalHours, semester));
        }
        return result;
    }

    private List<ParsedStudentRow> parseStudents(Sheet sheet, int disciplineCount, int studentStartRow) {
        List<ParsedStudentRow> students = new ArrayList<>();

        for (int rowIdx = studentStartRow; rowIdx <= sheet.getLastRowNum(); rowIdx++) {
            Row row = sheet.getRow(rowIdx);
            if (row == null) break;

            String rawName = getCellString(row.getCell(1));
            if (rawName == null || rawName.isBlank()) break;

            String name = rawName.replaceAll("[\\p{Z}\\s]+", " ").trim();

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
