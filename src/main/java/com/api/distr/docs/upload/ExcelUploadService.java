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

	public List<String> importExcel(MultipartFile file, Map<String, String> mappings, String companyName) throws Exception {

		List<String> allErrors = new ArrayList<>();
		Workbook wb = WorkbookFactory.create(file.getInputStream());
		Sheet sheet = wb.getSheetAt(0);

		for (int r = 1; r <= sheet.getLastRowNum(); r++) {
			Row row = sheet.getRow(r);
			if (row == null) continue;

			Map<String, Object> rowData = new HashMap<>();
			populate(mappings, rowData, row);

			try {
				SalesRecord daoRow = parseRow(rowData, companyName);
				List<String> rowErrors = updateDb(daoRow);
				for (String e : rowErrors)
					allErrors.add("Row " + (r + 1) + ": " + e);
			} catch (Exception ex) {
				allErrors.add("Row " + (r + 1) + ": " + ex.getMessage());
			}
		}

		wb.close();
		return allErrors;
	}

	public void excelMaster(MultipartFile file, Map<String, String> mappings,String type) throws Exception {

		String dateFormat = extractDateFormat(mappings);
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
				DeliveryExcelDTO daoRow = parseMaster(rowData, dateFormat);
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
			if (excelColumn.startsWith("_")) continue; // skip reserved keys like _dateFormat
			String fieldName = mappings.get(excelColumn);
			int colIndex = utils.excelColumnToIndex(excelColumn);
			Cell cell = row.getCell(colIndex);
			String value = utils.getCellValue(cell);
			rowData.put(fieldName, value);
		}
	}

	/** Extracts _dateFormat from mappings, defaulting to dd/MM/yyyy if absent. */
	private String extractDateFormat(Map<String, String> mappings) {
		String fmt = mappings.get("_dateFormat");
		return (fmt != null && !fmt.isBlank()) ? fmt : DistrConstants.DATE_FORMAT;
	}

	private List<String> updateDb(SalesRecord r) {
		List<String> errors = new ArrayList<>();
		try {
			custRepo.saveOrUpdate(r);
			if (repo.findCountBySalesOrder(r.getSalesOrderNo()) == 0) {
				repo.insert(r);
			}
		} catch (Exception ex) {
			errors.add("Row : " + ex.getMessage());
		}
		return errors;
	}
	 
	 private DeliveryExcelDTO parseMaster(Map<String, Object> row, String dateFormat) throws Exception {
		 DeliveryExcelDTO dto = new DeliveryExcelDTO();

		 // ── Core ──────────────────────────────────────────────────────────────
		 dto.setDeliveryName  (str(row, "Name"));
		 dto.setDeliveryMobile(str(row, "Mobile"));
		 dto.setAltMobile     (str(row, "Alternative Mobile"));
		 dto.setType          (str(row, "type"));

		 // ── Address ───────────────────────────────────────────────────────────
		 dto.setAddress1(str(row, "Address Line 1"));
		 dto.setAddress2(str(row, "Address Line 2"));
		 dto.setAddress3(str(row, "Address Line 3"));
		 dto.setCity    (str(row, "City"));
		 dto.setPinCode (str(row, "Pin Code"));
		 // keep legacy address column populated for backward compat
		 dto.setAddress (str(row, "Address Line 1"));

		 // ── Identity & Banking ────────────────────────────────────────────────
		 dto.setFatherName  (str(row, "Father Name"));
		 dto.setAadharNo    (str(row, "Aadhar No"));
		 dto.setPanCard     (str(row, "PAN Card"));
		 dto.setBankAccount (str(row, "Bank Account"));

		 // ── Date of Joining ───────────────────────────────────────────────────
		 String doj = str(row, "Date of Joining");
		 if (doj != null && !doj.isBlank()) {
			 try { dto.setDateOfJoining(Util.toSqlDate(doj, dateFormat)); }
			 catch (Exception ignored) { /* keep null if format doesn't match */ }
		 }

		 return dto;
	 }

	 /** Safely read a String value from the row map; returns null if missing or "null". */
	 private String str(Map<String, Object> row, String key) {
		 Object val = row.get(key);
		 if (val == null) return null;
		 String s = val.toString().trim();
		 return s.isEmpty() || s.equalsIgnoreCase("null") ? null : s;
	 }
	 
	 private CustomerDTO parseCustomer(Map<String, Object> row) throws Exception {
		 CustomerDTO dto = new CustomerDTO();
		 dto.setCustNo    (str(row, "Customer No"));
		 dto.setCustDesc  (str(row, "Customer Name"));
		 dto.setCustMobile(str(row, "Mobile"));

		 // Combine address lines into one field
		 String addr1 = str(row, "Address Line 1");
		 String addr2 = str(row, "Address Line 2");
		 String address = addr1 != null ? addr1 : "";
		 if (addr2 != null && !addr2.isBlank()) address = address + ", " + addr2;
		 dto.setAddress(address.isBlank() ? null : address);

		 dto.setPin(str(row, "Pin Code"));
		 dto.setLat(parseDoubleOrNull(str(row, "Lat")));
		 dto.setLon(parseDoubleOrNull(str(row, "Lon")));
		 return dto;
	 }

	 private Double parseDoubleOrNull(String s) {
		 if (s == null || s.isBlank()) return null;
		 try { return Double.parseDouble(s.replace(",", "").trim()); }
		 catch (NumberFormatException e) { return null; }
	 }
	 
	private SalesRecord parseRow(Map<String, Object> row, String companyName) throws Exception {
		SalesRecord r = new SalesRecord();

		String invoiceNo = str(row, "InvoiceNo");
		String picklistNo = str(row, "PicklistNo");
		r.setPicklistNo  (picklistNo != null ? picklistNo : "00000");
		r.setSalesOrderNo(invoiceNo);
		r.setCustomerNo  (str(row, "CustomerNo"));
		r.setCustDesc    (str(row, "CustomerName"));
		r.setSalesRepName(str(row, "SalesRepName"));
		r.setCompanyName (companyName);

		// ── Billing date — try multiple common formats ────────────────────────
		r.setBillingDate(parseSqlDate(str(row, "BillingDate")));

		// ── Net value — strip commas, handle blanks safely ────────────────────
		r.setNetValue(parseDouble(str(row, "NetValue")));

		return r;
	}

	/**
	 * Parse a date string trying several common formats.
	 * Also handles raw Excel date serial numbers (e.g. "46189").
	 * Returns null when the string is blank or unparseable.
	 */
	private java.sql.Date parseSqlDate(String s) {
		if (s == null || s.isBlank()) return null;
		String trimmed = s.trim();

		// Excel date serial number — numeric string with no separators
		if (trimmed.matches("\\d{4,6}(\\.\\d+)?")) {
			try {
				double serial = Double.parseDouble(trimmed);
				// Excel epoch: Dec 30 1899; POI's DateUtil handles the 1900 leap-year bug
				java.util.Date javaDate = org.apache.poi.ss.usermodel.DateUtil.getJavaDate(serial);
				return new java.sql.Date(javaDate.getTime());
			} catch (Exception ignored) { }
		}

		String[] formats = { "dd/MM/yyyy", "dd-MM-yyyy", "MM/dd/yyyy", "yyyy-MM-dd", "dd.MM.yyyy" };
		for (String fmt : formats) {
			try {
				java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat(fmt);
				sdf.setLenient(false);
				return new java.sql.Date(sdf.parse(trimmed).getTime());
			} catch (java.text.ParseException ignored) { }
		}
		System.err.println("Could not parse date: " + s);
		return null;
	}

	/** Parse a number string, stripping commas and spaces. Returns 0.0 on blank/error. */
	private double parseDouble(String s) {
		if (s == null || s.isBlank()) return 0.0;
		try {
			return Double.parseDouble(s.replace(",", "").trim());
		} catch (NumberFormatException e) {
			System.err.println("Could not parse number: " + s);
			return 0.0;
		}
	}
}
