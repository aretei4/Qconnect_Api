package com.api.distr.docs.sales.repo;

import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.jdbc.core.RowMapper;


import com.api.distr.docs.sales.dto.DeliveryAgent;
import com.api.distr.docs.sales.dto.DeliveryRequest;
import com.api.distr.docs.sales.dto.DeliveryStatus;
import com.api.distr.docs.sales.dto.SalesEntry;


import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;



@Repository
public class DeliveryRepository {

    private final JdbcTemplate jdbcTemplate;

    public DeliveryRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }
    
    public void upsertByPicklistNo(DeliveryStatus d) {
        String checkSql = "SELECT COUNT(*) FROM delivery_status WHERE picklist_no = ?";
        Integer count = jdbcTemplate.queryForObject(checkSql, Integer.class, d.getPicklistNo());

        if (count != null && count > 0) {
            // Update existing record
            String updateSql = """
                UPDATE delivery_status 
                SET delivered=?, otp=?, payment_amount=?, payment_mode=?, reason=? 
                WHERE picklist_no=?""";
            jdbcTemplate.update(updateSql,
                    d.isDelivered(),
                    d.isOtp(),
                    d.getPaymentAmount(),
                    d.getPaymentMode(),
                    d.getReason(),
                    d.getPicklistNo());
        } else {
            // Insert new record
            String insertSql = """
                INSERT INTO delivery_status (delivery_id, delivered, otp, payment_amount, payment_mode, picklist_no, reason)
                VALUES (?, ?, ?, ?, ?, ?, ?)""";
            jdbcTemplate.update(insertSql,
                    d.getDelivery_id(),
                    d.isDelivered(),
                    d.isOtp(),
                    d.getPaymentAmount(),
                    d.getPaymentMode(),
                    d.getPicklistNo(),
                    d.getReason());
        }
    }


    public void saveOrUpdate(DeliveryRequest request) {
        String sql = """
            INSERT INTO delivery_assignments 
            (picklist_no, delivery_boy_id, car_no, driver_name, mobile, updated_at)
            VALUES (?, ?, ?, ?, ?, ?)
            ON CONFLICT (picklist_no)
            DO UPDATE SET 
                delivery_boy_id = EXCLUDED.delivery_boy_id,
                car_no = EXCLUDED.car_no,
                driver_name = EXCLUDED.driver_name,
                mobile = EXCLUDED.mobile,
                updated_at = EXCLUDED.updated_at
        """;

        for (String picklistNo : request.getPicklistNos()) {
            jdbcTemplate.update(sql,
                    picklistNo,
                    request.getDeliveryBoyId(),
                    request.getCar().getCarNo(),
                    request.getCar().getDriverName(),
                    request.getCar().getMobile(),
                    LocalDateTime.now()
            );
        }
    }
    
    private static class DeliveryMapper implements RowMapper<DeliveryAgent> {
        @Override
        public DeliveryAgent mapRow(ResultSet rs, int rowNum) throws SQLException {
            DeliveryAgent d = new DeliveryAgent();
            d.setId(rs.getLong("delivery_id"));
            d.setName(rs.getString("delivery_name"));
            d.setContact(rs.getString("delivery_mobile"));
            d.setUpdatedDate(rs.getDate("updated_date") != null ? rs.getDate("updated_date").toLocalDate() : null);
            d.setActive(rs.getBoolean("active"));
            d.setBuId(rs.getInt("bu_id"));
            return d;
        }
    }
    public List<DeliveryAgent> findAll() {
        String sql = "SELECT * FROM delivery_master ORDER BY delivery_id";
        return jdbcTemplate.query(sql, new DeliveryMapper());
    }
    
  
    public List<SalesEntry> getAllSales() {
        String sql = """
            SELECT 
                picklist_no AS picklistNo,
                sales_order_no AS salesOrderNo,
                customer_no AS customerNo,
                cust_desc AS custDesc,
                sales_rep_no AS salesRepNo,
                sales_rep_name AS salesRepName,
                route,
                route_name AS routeName,
                billing_date AS billingDate,
                warehouse,
                net_value AS netValue,
                update_date AS updateDate,
                bu_id AS buId
            FROM stage_sales_entery
            ORDER BY update_date DESC
        """;
        return jdbcTemplate.query(sql, new BeanPropertyRowMapper<>(SalesEntry.class));
    }
}

