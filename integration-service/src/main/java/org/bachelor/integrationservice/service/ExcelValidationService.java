package org.bachelor.integrationservice.service;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ExcelValidationService {

    private static final int GROUP_ROW = 6;
    private static final int DISCIPLINE_START_COL = 2;

    private static final Pattern GROUP_PATTERN = Pattern.compile("групи\\s+\\S+");
    private static final Pattern SPECIALTY_PATTERN = Pattern.compile("спеціальності\\s+.+");

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
            int disciplineRow = detectDisciplineRow(sheet);
            validateYearRow(sheet, disciplineRow);
            validateDisciplineRow(sheet, disciplineRow);
            validateStudentRows(sheet, disciplineRow + 2);
        } finally {
            workbook.close();
        }
    }

    private int detectDisciplineRow(Sheet sheet) {
        for (int rowIdx = 8; rowIdx <= 12; rowIdx++) {
            Row row = sheet.getRow(rowIdx);
            if (row == null) continue;
            String cell = getCellString(row.getCell(DISCIPLINE_START_COL));
            if (cell != null && cell.contains("(дисц.)")) return rowIdx;
        }
        return 10; // default for zvit/zvit2
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

    private static final Pattern SINGLE_YEAR_PATTERN = Pattern.compile("(\\d{4})\\s*[\\-/–—]\\s*(\\d{4})");

    private void validateYearRow(Sheet sheet, int disciplineRow) {
        for (int rowIdx = 0; rowIdx <= Math.min(disciplineRow, sheet.getLastRowNum()); rowIdx++) {
            Row row = sheet.getRow(rowIdx);
            if (row == null) continue;
            for (int col = 0; col < row.getLastCellNum(); col++) {
                String text = getCellString(row.getCell(col));
                if (text == null) continue;
                Matcher m = SINGLE_YEAR_PATTERN.matcher(text);
                if (m.find()) {
                    int y1 = Integer.parseInt(m.group(1));
                    int y2 = Integer.parseInt(m.group(2));
                    if (y2 - y1 <= 2) return; // valid single academic year found
                }
            }
        }
        // Year not required for zvit3 format (graduation period spans >1 year)
    }

    private void validateDisciplineRow(Sheet sheet, int disciplineRow) {
        Row row = sheet.getRow(disciplineRow);
        if (row == null) {
            throw new IllegalArgumentException("Row " + (disciplineRow + 1) + " not found (expected to contain discipline names)");
        }

        String firstDiscipline = getCellString(row.getCell(DISCIPLINE_START_COL));
        if (firstDiscipline == null || firstDiscipline.isBlank()) {
            throw new IllegalArgumentException("Row " + (disciplineRow + 1) + " does not contain discipline names starting from column " + (DISCIPLINE_START_COL + 1));
        }
    }

    private void validateStudentRows(Sheet sheet, int studentStartRow) {
        Row firstStudentRow = sheet.getRow(studentStartRow);
        if (firstStudentRow == null) {
            throw new IllegalArgumentException("Row " + (studentStartRow + 1) + " not found (expected to contain student data)");
        }

        String studentName = getCellString(firstStudentRow.getCell(1));
        if (studentName == null || studentName.isBlank()) {
            throw new IllegalArgumentException("Row " + (studentStartRow + 1) + " does not contain student names (expected in column B)");
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
