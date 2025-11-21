package com.api.distr.docs.upload;

import java.sql.ResultSet;
import java.sql.Types;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class CustomerRepository {

    private final JdbcTemplate jdbc;

    public CustomerRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }
    public void saveOrUpdate(SalesRecord dto) {
    	CustomerDTO custDto = new CustomerDTO();
    	custDto.setCustDesc(dto.getCustDesc());
    	custDto.setCustNo(dto.getCustomerNo());
    	saveOrUpdate(custDto);
    }
    
    
    public void saveOrUpdate(CustomerDTO dto) {

        // Check if customer already exists
        String checkSql = "SELECT COUNT(*) FROM customer_details WHERE cust_no = ?";

        int count = jdbc.query(
                checkSql,
                new Object[]{dto.getCustNo()},
                (ResultSet rs) -> rs.next() ? rs.getInt(1) : 0
        );

        if (count > 0) {

            // UPDATE
            String updateSql = """
                UPDATE customer_details
                SET cust_desc = ?, cust_mobile = ?, address = ?, pin = ?, lat = ?, lon = ?
                WHERE cust_no = ?
                """;

            jdbc.update(conn -> {
                var ps = conn.prepareStatement(updateSql);
                ps.setString(1, dto.getCustDesc());
                ps.setString(2, dto.getCustMobile());
                ps.setString(3, dto.getAddress());
                ps.setString(4, dto.getPin());

                // DOUBLE fields → use setObject for nullable values
                ps.setObject(5, dto.getLat(), Types.DOUBLE);
                ps.setObject(6, dto.getLon(), Types.DOUBLE);

                ps.setString(7, dto.getCustNo());
                return ps;
            });

        } else {

            // INSERT
            String insertSql = """
                INSERT INTO customer_details
                (cust_no, cust_desc, cust_mobile, address, pin, lat, lon)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """;

            jdbc.update(conn -> {
                var ps = conn.prepareStatement(insertSql);
                ps.setString(1, dto.getCustNo());
                ps.setString(2, dto.getCustDesc());
                ps.setString(3, dto.getCustMobile());
                ps.setString(4, dto.getAddress());
                ps.setString(5, dto.getPin());

                ps.setObject(6, dto.getLat(), Types.DOUBLE);
                ps.setObject(7, dto.getLon(), Types.DOUBLE);

                return ps;
            });
        }
    }
}

