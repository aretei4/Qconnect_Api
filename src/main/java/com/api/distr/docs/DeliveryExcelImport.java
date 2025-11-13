package com.api.distr.docs;

import java.sql.*;
import java.time.LocalDate;
import java.io.*;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public class DeliveryExcelImport {

    private static final String DB_URL = "jdbc:postgresql://52.66.119.34:5432/distr";
    private static final String DB_USER = "postgres";
    private static final String DB_PASS = "Sachi@2010";

    public static void main(String[] args) {
    	   System.setProperty("user.timezone", "Asia/Kolkata");
        String excelPath = "D:/project/delivery_data.xlsx";
        DeliveryExcelImport importer = new DeliveryExcelImport();
        importer.process(excelPath);
    }

    // instance method so we can call from other classes
    public void process(String excelPath) {
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS)) {
          //  createTableIfNotExists(conn);
            importFromExcel(conn, excelPath);
            System.out.println("✅ Done import from " + excelPath);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void createTableIfNotExists(Connection conn) throws SQLException {
        String sql = """
            CREATE TABLE IF NOT EXISTS public.delivery_master (
                delivery_id BIGSERIAL PRIMARY KEY,
                delivery_name TEXT,
                delivery_mobile TEXT UNIQUE,
                updated_date DATE,
                active BOOLEAN DEFAULT TRUE,
                bu_id INTEGER DEFAULT 100
            )
            """;
        try (Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        }
    }

    private void importFromExcel(Connection conn, String filePath) {
        try (FileInputStream fis = new FileInputStream(new File(filePath));
             Workbook workbook = new XSSFWorkbook(fis)) {

            Sheet sheet = workbook.getSheetAt(0);
            boolean firstRow = true;

            for (Row row : sheet) {
                if (firstRow) { firstRow = false; continue; }

                String name = row.getCell(0).getStringCellValue().trim();
                String mobile = "";

                Cell mobileCell = row.getCell(1);
                if (mobileCell.getCellType() == CellType.NUMERIC) {
                    mobile = String.valueOf((long) mobileCell.getNumericCellValue());
                } else {
                    mobile = mobileCell.getStringCellValue().trim();
                }

                upsertDelivery(conn, name, mobile);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void upsertDelivery(Connection conn, String name, String mobile) {
        try {
            String checkSql = "SELECT COUNT(*) FROM delivery_master WHERE delivery_mobile = ?";
            try (PreparedStatement ps = conn.prepareStatement(checkSql)) {
                ps.setString(1, mobile);
                ResultSet rs = ps.executeQuery();
                rs.next();
                int count = rs.getInt(1);
                rs.close();

                if (count > 0) {
                    String updateSql = """
                        UPDATE delivery_master
                        SET delivery_name = ?, updated_date = ?
                        WHERE delivery_mobile = ?
                    """;
                    try (PreparedStatement ups = conn.prepareStatement(updateSql)) {
                        ups.setString(1, name);
                        ups.setDate(2, Date.valueOf(LocalDate.now()));
                        ups.setString(3, mobile);
                        ups.executeUpdate();
                        System.out.println("🔄 Updated: " + mobile);
                    }
                } else {
                    String insertSql = """
                        INSERT INTO delivery_master (delivery_name, delivery_mobile, updated_date)
                        VALUES (?, ?, ?)
                    """;
                    try (PreparedStatement ins = conn.prepareStatement(insertSql)) {
                        ins.setString(1, name);
                        ins.setString(2, mobile);
                        ins.setDate(3, Date.valueOf(LocalDate.now()));
                        ins.executeUpdate();
                        System.out.println("✅ Inserted: " + mobile);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

