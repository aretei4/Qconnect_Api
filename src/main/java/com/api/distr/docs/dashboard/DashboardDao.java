package com.api.distr.docs.dashboard;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

@Repository
public class DashboardDao {

	private final JdbcTemplate jdbcTemplate;

	public DashboardDao(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	public DeliverySummaryDto getDeliverySummary(String boyId) {

	    StringBuilder sql = new StringBuilder("""
	        SELECT
	            COUNT(*) AS total,
	            COUNT(*) FILTER (WHERE status = 2) AS delivered,
	            COUNT(*) FILTER (WHERE status = 0) AS pending,
	            COUNT(*) FILTER (WHERE status = 1) AS cancelled,

	            COUNT(*) FILTER (
	                WHERE delivery_date::date = CURRENT_DATE
	            ) AS today_total,

	            COUNT(*) FILTER (
	                WHERE status = 2
	                  AND delivery_date::date = CURRENT_DATE
	            ) AS today_delivered,

	            COUNT(*) FILTER (
	                WHERE status = 0
	                  AND delivery_date::date = CURRENT_DATE
	            ) AS today_pending,

	            COUNT(*) FILTER (
	                WHERE status = 1
	                  AND delivery_date::date = CURRENT_DATE
	            ) AS today_cancelled

	        FROM delivery_assignments
	        WHERE 1=1
	    """);

	    List<Object> params = new ArrayList<>();

	    // ✅ ADD CONDITION ONLY IF boyId IS PRESENT
	    if (boyId != null) {
	        sql.append(" AND delivery_boy_id = ?");
	        params.add(boyId);
	    }

	    return jdbcTemplate.queryForObject(
	        sql.toString(),
	        params.toArray(),
	        (rs, rowNum) -> {
	            DeliverySummaryDto dto = new DeliverySummaryDto();

	            dto.setTotalDeliveries(rs.getInt("total"));
	            dto.setDelivered(rs.getInt("delivered"));
	            dto.setPending(rs.getInt("pending"));
	            dto.setCancelled(rs.getInt("cancelled"));

	            dto.setTodayTotal(rs.getInt("today_total"));
	            dto.setTodayDelivered(rs.getInt("today_delivered"));
	            dto.setTodayPending(rs.getInt("today_pending"));
	            dto.setTodayCancelled(rs.getInt("today_cancelled"));

	            return dto;
	        }
	    );
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
			params = new Object[] {};
		} else {
			sql = """
					    SELECT id, customer_name, address, status
					    FROM deliveries
					    WHERE status = ?
					    ORDER BY id DESC
					""";
			params = new Object[] { status };
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
