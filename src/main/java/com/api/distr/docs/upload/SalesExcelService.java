package com.api.distr.docs.upload;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.api.distr.docs.jwt.DistrConstants;
import com.api.distr.docs.jwt.Util;

import java.sql.Date;
import java.util.ArrayList;
import java.util.List;

@Service
public class SalesExcelService {

    private final SalesExcelRepository repo;
    
    @Autowired
    CustomerRepository custRepo;
    
    public SalesExcelService(SalesExcelRepository repo) {
        this.repo = repo;
    }
    public List<CustomerDTO> getAllCustomers() {
        return custRepo.findAll();
    }
    
    public void saveOrUpdateCustomer(CustomerDTO dto) {
        // extra validations if needed
        if (dto.getCustNo() == null || dto.getCustNo().isBlank()) {
            throw new RuntimeException("Customer number is required");
        }

        custRepo.saveOrUpdate(dto);
    }
    
    public List<String> processSalesExcel(MultipartFile file) {
        List<String> errors = new ArrayList<>();

        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {

            Sheet sheet = workbook.getSheetAt(0);
            int rows = sheet.getPhysicalNumberOfRows();

            for (int i = 1; i < rows; i++) { // skip header
                Row row = sheet.getRow(i);
                if (row == null) continue;

                try {
                    SalesRecord r = parseRow(row);
                    custRepo.saveOrUpdate(r);
                    if (repo.findCountByPicklist(r.getPicklistNo()) > 0) {
                        repo.update(r);
                    } else {
                        repo.insert(r);
                    }

                } catch (Exception ex) {
                    errors.add("Row " + (i + 1) + ": " + ex.getMessage());
                }
            }

        } catch (Exception e) {
            errors.add("Failed to process file: " + e.getMessage());
        }

        return errors;
    }

    private SalesRecord parseRow(Row row) throws Exception {
        SalesRecord r = new SalesRecord();

        r.setPicklistNo(getString(row.getCell(0)));
        r.setSalesOrderNo(getString(row.getCell(1)));
        r.setCustomerNo(getString(row.getCell(2)));
        r.setCustDesc(getString(row.getCell(3)));
        r.setSalesRepNo(getString(row.getCell(4)));
        r.setSalesRepName(getString(row.getCell(5)));
        r.setRoute(getString(row.getCell(6)));
        r.setRouteName(getString(row.getCell(7)));
        
        
        String billingDateStr = getString(row.getCell(8));
        java.sql.Date billingDate = Util.toSqlDate(billingDateStr, DistrConstants.DATE_FORMAT);//(java.sql.Date) new Date( getStringCellValue(row.getCell(8)));
        r.setBillingDate(billingDate);  // YYYY-MM-DD

        r.setWarehouse(getString(row.getCell(9)));
        r.setNetValue(Double.parseDouble(getString(row.getCell(10))));

        return r;
    }

    private String getString(Cell cell) {
        if (cell == null) return "";
        if (cell.getCellType() == CellType.NUMERIC)
            return String.valueOf((long) cell.getNumericCellValue());
        return cell.getStringCellValue().trim();
    }
}

