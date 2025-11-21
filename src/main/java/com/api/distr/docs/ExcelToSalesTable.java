package com.api.distr.docs;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import com.api.distr.docs.jwt.DistrConstants;
import com.api.distr.docs.jwt.Util;

import java.io.FileInputStream;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ExcelToSalesTable {

    public static void main(String[] args) {
        String excelFilePath = "D:/project/sales_picklist.xlsx"; // Path to Excel file
        String jdbcUrl = "jdbc:postgresql://52.66.119.34:5432/distr";
        String username = "postgres";
        String password = "Sachi@2010";
        
        System.setProperty("user.timezone", "Asia/Kolkata");
        
        try (Connection conn = DriverManager.getConnection(jdbcUrl, username, password)) {
            conn.setAutoCommit(false);

            try (
            		FileInputStream fis = new FileInputStream(excelFilePath);
                 Workbook workbook = new XSSFWorkbook(fis)) {

                Sheet sheet = workbook.getSheetAt(0);
                int rows = sheet.getPhysicalNumberOfRows();

                String selectSql = DistrConstants.STAGE_SELECT_COUNT;
                String insertSql = DistrConstants.STAGE_INSERT;       
                String updateSql = DistrConstants.STAGE_UPDATE;

                PreparedStatement psSelect = conn.prepareStatement(selectSql);
                PreparedStatement psInsert = conn.prepareStatement(insertSql);
                PreparedStatement psUpdate = conn.prepareStatement(updateSql);

                //SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

                for (int i = 1; i < rows; i++) { // Skip header
                    Row row = sheet.getRow(i);
                    if (row == null) continue;

                    try {
                        String picklistNo = getStringCellValue(row.getCell(0));
                        String salesOrderNo = getStringCellValue(row.getCell(1));
                        String customerNo = getStringCellValue(row.getCell(2));
                        String custDesc = getStringCellValue(row.getCell(3));
                        String salesRepNo = getStringCellValue(row.getCell(4));
                        String salesRepName = getStringCellValue(row.getCell(5));
                        String route = getStringCellValue(row.getCell(6));
                        String routeName = getStringCellValue(row.getCell(7));
                        String billingDate1 = getStringCellValue(row.getCell(8));
                        java.sql.Date billingDate = Util.toSqlDate(billingDate1, DistrConstants.DATE_FORMAT);//(java.sql.Date) new Date( getStringCellValue(row.getCell(8)));
                        String warehouse = getStringCellValue(row.getCell(9));
                        double netValue = Double.parseDouble(getStringCellValue(row.getCell(10)));
                        

                        if (picklistNo == null || picklistNo.isEmpty()) {
                            System.out.println("⚠️ Skipped row " + (i + 1) + ": Picklist_No is empty.");
                            continue;
                        }

                        // Check if record exists
                        psSelect.setString(1, picklistNo);
                        ResultSet rs = psSelect.executeQuery();
                        rs.next();
                        int count = rs.getInt(1);

                        if (count > 0) {
                            // Update existing record
                            psUpdate.setString(1, salesOrderNo);
                            psUpdate.setString(2, customerNo);
                            psUpdate.setString(3, custDesc);
                            psUpdate.setString(4, salesRepNo);
                            psUpdate.setString(5, salesRepName);
                            psUpdate.setString(6, route);
                            psUpdate.setString(7, routeName);
                            psUpdate.setDate(8, billingDate);
                            psUpdate.setString(9, warehouse);
                            psUpdate.setDouble(10, netValue);
                            psUpdate.setString(11, picklistNo);
                            psUpdate.executeUpdate();

                            System.out.println("🔁 Updated record for Picklist_No: " + picklistNo);
                        } else {
                            // Insert new record
                            psInsert.setString(1, picklistNo);
                            psInsert.setString(2, salesOrderNo);
                            psInsert.setString(3, customerNo);
                            psInsert.setString(4, custDesc);
                            psInsert.setString(5, salesRepNo);
                            psInsert.setString(6, salesRepName);
                            psInsert.setString(7, route);
                            psInsert.setString(8, routeName);
                            psInsert.setDate(9, billingDate);
                            psInsert.setString(10, warehouse);
                            psInsert.setDouble(11, netValue);
                            psInsert.executeUpdate();

                            System.out.println("✅ Inserted record for Picklist_No: " + picklistNo);
                        }
                    } catch (Exception ex) {
                        System.err.println("❌ Error processing row " + (i + 1) + ": " + ex.getMessage());
                    }
                }

                conn.commit();
                System.out.println("\n🎯 Excel data import completed successfully!");
            } catch (Exception e) {
                conn.rollback();
                e.printStackTrace();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static String getStringCellValue(Cell cell) {
        if (cell == null) return null;
        if (cell.getCellType() == CellType.NUMERIC) {
            return String.valueOf((long) cell.getNumericCellValue());
        }
        return cell.getStringCellValue().trim();
    }

    private static double getNumericCellValue(Cell cell) {
        if (cell == null) return 0.0;
        if (cell.getCellType() == CellType.STRING) {
            try {
                return Double.parseDouble(cell.getStringCellValue());
            } catch (NumberFormatException e) {
                return 0.0;
            }
        }
        return cell.getNumericCellValue();
    }

    private static java.sql.Date getDateCellValue(Cell cell) {
        if (cell == null) return null;
        if (DateUtil.isCellDateFormatted(cell)) {
            Date date = cell.getDateCellValue();
            return new java.sql.Date(date.getTime());
        }
        return null;
    }
}

