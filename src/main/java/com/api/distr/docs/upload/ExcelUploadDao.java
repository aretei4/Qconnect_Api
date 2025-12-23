package com.api.distr.docs.upload;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class ExcelUploadDao {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    public void saveCustomerRow(Map<String, Object> row) {

        String sql = """
            INSERT INTO customer_upload 
            (customer_name, lon, lat, pin)
            VALUES (?, ?, ?, ?)
        """;

        jdbcTemplate.update(sql,
                row.get("customerName"),
                parseDouble(row.get("lon")),
                parseDouble(row.get("lat")),
                row.get("pin")
        );
    }

    private Double parseDouble(Object value) {
        try { return value != null ? Double.parseDouble(value.toString()) : null; }
        catch (Exception e) { return null; }
    }
}

