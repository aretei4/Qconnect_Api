package com.api.distr.docs.dashboard;


import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class DashboardDao {

    private final JdbcTemplate jdbcTemplate;

    public DashboardDao(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public DeliverySummaryDto getDeliverySummary() {

        String sql = """
            SELECT 
        		COUNT(*) AS total,
        		COUNT(*) FILTER (WHERE delivered=true) AS delivered,
                COUNT(*) FILTER (WHERE delivered=true) AS pending,
                COUNT(*) FILTER (WHERE delivered=false) AS cancelled
        FROM delivery_status
        """;

        return jdbcTemplate.queryForObject(sql, (rs, rowNum) -> {
            DeliverySummaryDto dto = new DeliverySummaryDto();
            dto.setTotalDeliveries(rs.getInt("total"));
            dto.setDelivered(rs.getInt("delivered"));
            dto.setPending(rs.getInt("pending"));
            dto.setCancelled(rs.getInt("cancelled"));
            return dto;
        });
    }

    public List<DeliveryDetailsDto> getDeliveryDetails(String status) {

        String sql;
        Object[] params;

        if ("ALL".equalsIgnoreCase(status)) {
            sql = """
                SELECT id, customer_name, address, status
                FROM deliveries
                ORDER BY id DESC
            """;
            params = new Object[]{};
        } else {
            sql = """
                SELECT id, customer_name, address, status
                FROM deliveries
                WHERE status = ?
                ORDER BY id DESC
            """;
            params = new Object[]{status};
        }

        return jdbcTemplate.query(sql, params, (rs, rowNum) -> {
            DeliveryDetailsDto dto = new DeliveryDetailsDto();
            dto.setId(rs.getLong("id"));
            dto.setCustomerName(rs.getString("customer_name"));
            dto.setAddress(rs.getString("address"));
            dto.setStatus(rs.getString("status"));
            return dto;
        });
    }
}

