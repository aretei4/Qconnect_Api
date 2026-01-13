package com.api.distr.docs.upload;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.api.distr.docs.sales.dto.DeliveryStatus;

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
    private Double getNullableDouble(ResultSet rs, String column) throws SQLException {
        String value = rs.getString(column);
        return (value == null || value.isBlank()) ? 0.00 : Double.parseDouble(value);
    }
    
    public int updateCustomerLatLon(
    		DeliveryStatus deliVery) {

        String sql = """
            UPDATE customer_details
            SET lat = ?, lon = ?
            WHERE cust_no IN (
                SELECT customer_no
                FROM stage_sales_entery
                WHERE picklist_no = ?
            )
        """;

        return jdbc.update(sql, deliVery.getLat(), deliVery.getLon(), deliVery.getPicklistNo());
    }

    public List<CustomerDTO> findAll() {

        String sql = """
            SELECT cust_id, cust_no, cust_desc, cust_mobile,
                   TO_CHAR(updated_date, 'DD/MM/YYYY') AS updated_date,
                   bu_id, lat, lon, address, pin
            FROM customer_details
            ORDER BY updated_date DESC
        """;

        return jdbc.query(sql, (rs, rowNum) -> {
            CustomerDTO dto = new CustomerDTO();
          //  dto.setCustId(rs.getLong("cust_id"));
            dto.setCustNo(rs.getString("cust_no"));
            dto.setCustDesc(rs.getString("cust_desc"));
            dto.setCustMobile(rs.getString("cust_mobile"));
            //dto.setUpdatedDate(rs.getString("updated_date"));
            //dto.setBuId(rs.getInt("bu_id"));
            dto.setLat(getNullableDouble(rs, "lat"));
            dto.setLon(getNullableDouble(rs, "lon"));
       
            dto.setAddress(rs.getString("address"));
            dto.setPin(rs.getString("pin"));
            return dto;
        });
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

