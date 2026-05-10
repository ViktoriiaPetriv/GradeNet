package org.bachelor.integrationservice.service;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.regex.Pattern;

@Service
public class ExcelValidationService {

    private static final int GROUP_ROW = 6;
    private static final int YEAR_ROW = 7;
    private static final int DISCIPLINE_ROW = 10;
    private static final int STUDENT_START_ROW = 12;
    private static final int DISCIPLINE_START_COL = 2;

    private static final Pattern GROUP_PATTERN = Pattern.compile("групи\\s+\\S+");
    private static final Pattern SPECIALTY_PATTERN = Pattern.compile("спеціальності\\s+.+");
    private static final Pattern YEAR_PATTERN = Pattern.compile("(\\d{4})\\s*[\\-/–—]\\s*(\\d{4})");

    public void validate(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }

        String fileName = file.getOriginalFilename();
        if (fileName == null || !fileName.endsWith(".xlsx")) {
            throw new IllegalArgumentException("File must be in .xlsx format (Excel 2007+)");
        }

        try (InputStream inputStream = file.getInputStream()) {
            validateContent(inputStream);
        }
    }

    private void validateContent(InputStream inputStream) throws IOException {
        Workbook workbook;
        try {
            workbook = new XSSFWorkbook(inputStream);
        } catch (IOException e) {
            throw new IllegalArgumentException("File is not a valid Excel (.xlsx) file: " + e.getMessage(), e);
        }

        try {
            if (workbook.getNumberOfSheets() == 0) {
                throw new IllegalArgumentException("Excel file does not contain any sheets");
            }

            Sheet sheet = workbook.getSheetAt(0);
            validateGroupRow(sheet);
            validateYearRow(sheet);
            validateDisciplineRow(sheet);
            validateStudentRows(sheet);
        } finally {
            workbook.close();
        }
    }

    private void validateGroupRow(Sheet sheet) {
        Row row = sheet.getRow(GROUP_ROW);
        if (row == null) {
            throw new IllegalArgumentException("Row " + (GROUP_ROW + 1) + " not found (expected to contain group and specialty info)");
        }

        String text = getCellString(row.getCell(0));
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("Row " + (GROUP_ROW + 1) + " cell A is empty (expected to contain group name and specialty)");
        }

        if (!GROUP_PATTERN.matcher(text).find()) {
            throw new IllegalArgumentException("Row " + (GROUP_ROW + 1) + " does not match expected group pattern (expected: 'групи <GroupName>')");
        }

        if (!SPECIALTY_PATTERN.matcher(text).find()) {
            throw new IllegalArgumentException("Row " + (GROUP_ROW + 1) + " does not match expected specialty pattern (expected: 'спеціальності <SpecialtyName>')");
        }
    }

    private void validateYearRow(Sheet sheet) {
        boolean yearFound = false;
        for (int rowIdx = 0; rowIdx <= Math.min(DISCIPLINE_ROW, sheet.getLastRowNum()); rowIdx++) {
            Row row = sheet.getRow(rowIdx);
            if (row == null) continue;
            for (int col = 0; col < row.getLastCellNum(); col++) {
                String text = getCellString(row.getCell(col));
                if (text != null && YEAR_PATTERN.matcher(text).find()) {
                    yearFound = true;
                    break;
                }
            }
            if (yearFound) break;
        }

        if (!yearFound) {
            throw new IllegalArgumentException("Academic year not found in expected format (expected: YYYY-YYYY, YYYY/YYYY, or similar)");
        }
    }

    private void validateDisciplineRow(Sheet sheet) {
        Row row = sheet.getRow(DISCIPLINE_ROW);
        if (row == null) {
            throw new IllegalArgumentException("Row " + (DISCIPLINE_ROW + 1) + " not found (expected to contain discipline names)");
        }

        String firstDiscipline = getCellString(row.getCell(DISCIPLINE_START_COL));
        if (firstDiscipline == null || firstDiscipline.isBlank()) {
            throw new IllegalArgumentException("Row " + (DISCIPLINE_ROW + 1) + " does not contain discipline names starting from column " + (DISCIPLINE_START_COL + 1));
        }
    }

    private void validateStudentRows(Sheet sheet) {
        Row firstStudentRow = sheet.getRow(STUDENT_START_ROW);
        if (firstStudentRow == null) {
            throw new IllegalArgumentException("Row " + (STUDENT_START_ROW + 1) + " not found (expected to contain student data)");
        }

        String studentName = getCellString(firstStudentRow.getCell(1));
        if (studentName == null || studentName.isBlank()) {
            throw new IllegalArgumentException("Row " + (STUDENT_START_ROW + 1) + " does not contain student names (expected in column B)");
        }
    }

    private String getCellString(org.apache.poi.ss.usermodel.Cell cell) {
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
}
