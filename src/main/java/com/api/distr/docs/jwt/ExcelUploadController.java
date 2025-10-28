package com.api.distr.docs.jwt;



import org.apache.poi.ss.usermodel.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;

@RestController
@RequestMapping("/api/excel")
public class ExcelUploadController {

    // ✅ Required columns (adjust as needed)
    private static final List<String> REQUIRED_HEADERS = DistrConstants.REQUIRED_HEADERS;

    @PostMapping("/upload")
    public ResponseEntity<?> uploadExcel(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Please upload a file."));
        }

        if (!file.getOriginalFilename().endsWith(".xlsx") && !file.getOriginalFilename().endsWith(".xls")) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid file type. Please upload an Excel file."));
        }

        try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            if (sheet == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Excel file is empty or invalid."));
            }

            Row headerRow = sheet.getRow(0);
            if (headerRow == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Missing header row in Excel file."));
            }

            // ✅ Read headers
            List<String> headers = new ArrayList<>();
            for (Cell cell : headerRow) {
                headers.add(cell.getStringCellValue().trim());
            }

            // ✅ Validate required headers
            for (String required : REQUIRED_HEADERS) {
                if (!headers.contains(required)) {
                    return ResponseEntity.badRequest().body(
                        Map.of("error", "Missing required column: " + required)
                    );
                }
            }

            List<Map<String, Object>> rows = new ArrayList<>();
            List<String> validationErrors = new ArrayList<>();

            // ✅ Process each row
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                Map<String, Object> rowData = new LinkedHashMap<>();
                boolean isEmptyRow = true;

                for (int j = 0; j < headers.size(); j++) {
                    String header = headers.get(j);
                    Cell cell = row.getCell(j);
                    Object value = getCellValue(cell);

                    // ✅ Check null or blank
                    if (value == null || value.toString().trim().isEmpty()) {
                        validationErrors.add("Row " + (i + 1) + ": Column '" + header + "' cannot be blank");
                    }

                    rowData.put(header, value);

                    if (value != null && !value.toString().isBlank()) {
                        isEmptyRow = false;
                    }
                }

                if (!isEmptyRow) {
                    rows.add(rowData);
                }
            }

            Map<String, Object> response = new HashMap<>();
            response.put("fileName", file.getOriginalFilename());
            response.put("totalRows", rows.size());
            response.put("preview", rows.subList(0, Math.min(rows.size(), 5)));

            if (!validationErrors.isEmpty()) {
                response.put("validationErrors", validationErrors);
            }

            return ResponseEntity.ok(response);

        } catch (IOException e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Error reading Excel file: " + e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Unexpected error: " + e.getMessage()));
        }
    }

    // ✅ Helper: extract cell value safely
    private Object getCellValue(Cell cell) {
        if (cell == null) return null;
        switch (cell.getCellType()) {
        case STRING:
            return cell.getStringCellValue();
        case NUMERIC:
            return cell.getNumericCellValue();
        case BOOLEAN:
            return cell.getBooleanCellValue();
        default:
            return null;
    }
}
}
