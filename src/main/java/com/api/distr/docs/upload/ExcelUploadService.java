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
	private DeliveryExcelRepository excelRepo;


	
	@Autowired
	private ExcelUtils utils;

	public void importExcel(MultipartFile file, Map<String, String> mappings) throws Exception {

		Workbook wb = WorkbookFactory.create(file.getInputStream());
		Sheet sheet = wb.getSheetAt(0);

		// Loop through all rows (skip header)
		for (int r = 1; r <= sheet.getLastRowNum(); r++) {

			Row row = sheet.getRow(r);
			if (row == null)
				continue;

			Map<String, Object> rowData = new HashMap<>();
			populate(mappings, rowData, row);
			// Process mapping: "E" -> "customerName"
			/*
			 * for (String excelColumn : mappings.keySet()) {
			 * 
			 * String fieldName = mappings.get(excelColumn); int colIndex =
			 * utils.excelColumnToIndex(excelColumn); Cell cell = row.getCell(colIndex);
			 * String value = utils.getCellValue(cell); rowData.put(fieldName, value); }
			 */

			SalesRecord daoRow = parseRow(rowData);
			updateDb(daoRow);
			// Save row via DAO
			// dao.saveCustomerRow(rowData);
		}

		wb.close();
	}

	public void excelMaster(MultipartFile file, Map<String, String> mappings,String type) throws Exception {

		Workbook wb = WorkbookFactory.create(file.getInputStream());
		Sheet sheet = wb.getSheetAt(0);

		// Loop through all rows (skip header)
		for (int r = 1; r <= sheet.getLastRowNum(); r++) {

			Row row = sheet.getRow(r);
			if (row == null)
				continue;

			Map<String, Object> rowData = new HashMap<>();
			populate(mappings, rowData, row);
			rowData.put("type", type);
			if(type.equalsIgnoreCase("c")) {
				CustomerDTO daoRow = parseCustomer(rowData);
				custRepo.saveOrUpdate(daoRow);
			}else {
				DeliveryExcelDTO daoRow = parseMaster(rowData);
				excelRepo.saveOrUpdate(daoRow);
			}
			
			//updateDb(daoRow);
			// Save row via DAO
			// dao.saveCustomerRow(rowData);
		}

		wb.close();
	}
	
	private void populate(Map<String, String> mappings, Map<String, Object> rowData, Row row) {
		for (String excelColumn : mappings.keySet()) {

			String fieldName = mappings.get(excelColumn);
			int colIndex = utils.excelColumnToIndex(excelColumn);
			Cell cell = row.getCell(colIndex);
			String value = utils.getCellValue(cell);
			rowData.put(fieldName, value);
		}

	}

	private List<String> updateDb(SalesRecord r) {
		List<String> errors = new ArrayList<>();
		try {
			// SalesRecord r = parseRow(row);
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
	 
	 private DeliveryExcelDTO parseMaster(Map<String, Object> row) throws Exception {
		 DeliveryExcelDTO dto = new DeliveryExcelDTO();
		 dto.setAddress(""+row.get("address"));
		 dto.setDeliveryName(""+row.get("Name"));
		 dto.setDeliveryMobile(""+row.get("Mobile"));
		 dto.setType(""+row.get("type"));
		 dto.setLat(Double.parseDouble(""+row.get("lat")));
		 dto.setLon(Double.parseDouble(""+row.get("lon")));
		// dto.setUpdatedDate(""+row.get("address"));
		 dto.setPin(""+row.get("pin"));
		 // master: ["", "Mobile", "address", "lat", "lon", "pin"]
		 return dto;
	 }
	 
	 private CustomerDTO parseCustomer(Map<String, Object> row) throws Exception {
		 CustomerDTO dto = new CustomerDTO();
		 dto.setAddress(""+row.get("address"));
		 dto.setCustDesc(""+row.get("Name"));
		 dto.setCustMobile(""+row.get("Mobile"));
		 dto.setCustNo(""+row.get("CustNo"));
		// dto.setType(""+row.get("type"));
		 dto.setLat(Double.parseDouble(""+row.get("lat")));
		 dto.setLon(Double.parseDouble(""+row.get("lon")));
		// dto.setUpdatedDate(""+row.get("address"));
		 dto.setPin(""+row.get("pin"));
		 // master: ["", "Mobile", "address", "lat", "lon", "pin"]
		 return dto;
	 }
	 
	private SalesRecord parseRow(Map<String, Object> row) throws Exception {
		SalesRecord r = new SalesRecord();

		r.setPicklistNo("" + row.get("PicklistNo"));
		r.setCustomerNo("" + row.get("CustomerNo"));
		r.setCustDesc("" + row.get("CustomerName"));
		
		String billingDateStr = "" + row.get("BillingDate");
		java.sql.Date billingDate = Util.toSqlDate(billingDateStr, DistrConstants.DATE_FORMAT);// (java.sql.Date) new
																								// Date(
																								// getStringCellValue(row.getCell(8)));
		r.setBillingDate(billingDate); // YYYY-MM-DD		
		r.setNetValue(Double.parseDouble("" + row.get("NetValue")));

		return r;
	}
}
