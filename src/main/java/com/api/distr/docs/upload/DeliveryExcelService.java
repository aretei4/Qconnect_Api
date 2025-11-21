package com.api.distr.docs.upload;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.sql.Date;
import java.util.ArrayList;
import java.util.List;

@Service
public class DeliveryExcelService {

    private final DeliveryExcelRepository repository;

    public DeliveryExcelService(DeliveryExcelRepository repository) {
        this.repository = repository;
    }

    public List<String> uploadExcel(MultipartFile file) {
        List<String> errors = new ArrayList<>();

        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {

            Sheet sheet = workbook.getSheetAt(0);
            int rows = sheet.getPhysicalNumberOfRows();

            for (int i = 1; i < rows; i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                try {
                    DeliveryExcelDTO dto = new DeliveryExcelDTO();
                    dto.setDeliveryName(getString(row.getCell(0)));
                    dto.setDeliveryMobile(getString(row.getCell(1)));
                    dto.setUpdatedDate(new Date(System.currentTimeMillis()));
                    dto.setLat(toDouble("19.76"));
                    dto.setLon(toDouble("72.8777"));
                    dto.setAddress(getString(row.getCell(5)));

                    if (dto.getDeliveryMobile() == null || dto.getDeliveryMobile().isEmpty()) {
                        errors.add("Row " + (i + 1) + ": Mobile number missing");
                        continue;
                    }

                    int count = repository.countByMobile(dto.getDeliveryMobile());

                    if (count > 0) {
                        repository.update(dto);
                    } else {
                        repository.insert(dto);
                    }

                } catch (Exception ex) {
                    errors.add("Row " + (i + 1) + ": " + ex.getMessage());
                }
            }

        } catch (Exception ex) {
            errors.add("File Error: " + ex.getMessage());
        }

        return errors;
    }

    private String getString(Cell cell) {
        if (cell == null) return null;
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue().trim();
            case NUMERIC -> String.valueOf(cell.getNumericCellValue());
            default -> null;
        };
    }

    private Double toDouble(String v) {
        try {
            return v == null ? null : Double.parseDouble(v);
        } catch (Exception e) {
            return null;
        }
    }

    private Date toSqlDate(String value) {
        try {
            return value == null ? null : Date.valueOf(value);
        } catch (Exception e) {
            return null;
        }
    }
}

