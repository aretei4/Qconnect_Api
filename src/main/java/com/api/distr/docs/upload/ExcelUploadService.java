package com.api.distr.docs.upload;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.api.distr.docs.jwt.DistrConstants;
import com.api.distr.docs.jwt.Util;

@Service
public class ExcelUploadService {
	
    @Autowired
    CustomerRepository custRepo;
    
    @Autowired
    SalesExcelRepository repo;
    

    @Autowired
    private ExcelUploadDao dao;

    @Autowired
    private ExcelUtils utils;

    public void importExcel(MultipartFile file, Map<String, String> mappings) throws Exception {

        Workbook wb = WorkbookFactory.create(file.getInputStream());
        Sheet sheet = wb.getSheetAt(0);

        // Loop through all rows (skip header)
        for (int r = 1; r <= sheet.getLastRowNum(); r++) {

            Row row = sheet.getRow(r);
            if (row == null) continue;

            Map<String, Object> rowData = new HashMap<>();

            // Process mapping: "E" -> "customerName"
            for (String excelColumn : mappings.keySet()) {

                String fieldName = mappings.get(excelColumn);
                int colIndex = utils.excelColumnToIndex(excelColumn);

                Cell cell = row.getCell(colIndex);
                String value = utils.getCellValue(cell);

                rowData.put(fieldName, value);
            }
            SalesRecord daoRow = parseRow(rowData);
            updateDb(daoRow);
            // Save row via DAO
           // dao.saveCustomerRow(rowData);
        }

        wb.close();
    }
    
    private List<String> updateDb(SalesRecord r) {
    	   List<String> errors = new ArrayList<>();
    	try {
          //  SalesRecord r = parseRow(row);
            custRepo.saveOrUpdate(r);
            if (repo.findCountByPicklist(r.getPicklistNo()) > 0) {
                repo.update(r);
            } else {
                repo.insert(r);
            }

        } catch (Exception ex) {
            errors.add("Row : " + ex.getMessage());
        }
    	return errors;
    }





    	
   
    private SalesRecord parseRow(Map<String, Object> row ) throws Exception {
        SalesRecord r = new SalesRecord();

        r.setPicklistNo(""+row.get("PicklistNo"));
      //  r.setSalesOrderNo(getString(row.getCell(1)));
        r.setCustomerNo(""+row.get("CustomerNo"));
        r.setCustDesc(""+row.get("CustomerName"));
        //r.setSalesRepNo(getString(row.getCell(4)));
        //r.setSalesRepName(getString(row.getCell(5)));
        //r.setRoute(getString(row.getCell(6)));
        //r.setRouteName(getString(row.getCell(7)));
        
        
        String billingDateStr = ""+row.get("BillingDate");
        java.sql.Date billingDate = Util.toSqlDate(billingDateStr, DistrConstants.DATE_FORMAT);//(java.sql.Date) new Date( getStringCellValue(row.getCell(8)));
        r.setBillingDate(billingDate);  // YYYY-MM-DD

        //r.setWarehouse(getString(row.getCell(9)));
        r.setNetValue(Double.parseDouble(""+row.get("NetValue")));

        return r;
    }
}

