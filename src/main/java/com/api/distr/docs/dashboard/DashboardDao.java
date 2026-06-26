package com.api.distr.docs.dashboard;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

@Repository
public class DashboardDao {

	private static final Logger log = LoggerFactory.getLogger(DashboardDao.class);

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

	    DeliverySummaryDto dto = jdbcTemplate.queryForObject(
	        sql.toString(),
	        params.toArray(),
	        (rs, rowNum) -> {
	            DeliverySummaryDto d = new DeliverySummaryDto();
	            d.setTotalDeliveries(rs.getInt("total"));
	            d.setDelivered(rs.getInt("delivered"));
	            d.setPending(rs.getInt("pending"));
	            d.setCancelled(rs.getInt("cancelled"));
	            d.setTodayTotal(rs.getInt("today_total"));
	            d.setTodayDelivered(rs.getInt("today_delivered"));
	            d.setTodayPending(rs.getInt("today_pending"));
	            d.setTodayCancelled(rs.getInt("today_cancelled"));
	            return d;
	        }
	    );

	    // ── Net value + collected amount ──────────────────────────────────────────
	    // Base: delivery_assignments (da) — stable, already used above.
	    //   • net_value  : da.dire_id  → stage_sales_entery.dire_id  (both original columns)
	    //   • today filter: da.delivery_date (set to CURRENT_TIMESTAMP on each delivery update)
	    //   • payment    : LEFT JOIN delivery_status ON picklist_no (original column in both tables)
	    //                  ds.payment_amount / ds.delivered are original columns — no ALTER needed
	    // This avoids touching delivery_status.dire_id / delivery_status.delivery_date
	    // which may not exist in the production DB yet.
	    StringBuilder amtSql = new StringBuilder(
	        "SELECT " +
	        "  COALESCE(SUM(CAST(sse.net_value AS double precision)) " +
	        "      FILTER (WHERE da.delivery_date IS NOT NULL " +
	        "                AND da.delivery_date::date = CURRENT_DATE), 0)  AS today_net_value, " +
	        "  COALESCE(SUM(CAST(sse.net_value AS double precision)), 0)     AS total_net_value, " +
	        "  COALESCE(SUM(ds.payment_amount) " +
	        "      FILTER (WHERE da.delivery_date IS NOT NULL " +
	        "                AND da.delivery_date::date = CURRENT_DATE " +
	        "                AND ds.delivered = true), 0)                    AS today_collected, " +
	        "  COALESCE(SUM(ds.payment_amount) " +
	        "      FILTER (WHERE ds.delivered = true), 0)                    AS total_collected " +
	        "FROM delivery_assignments da " +
	        "LEFT JOIN stage_sales_entery sse ON sse.dire_id = da.dire_id " +
	        "LEFT JOIN delivery_status   ds  ON ds.picklist_no = da.picklist_no " +
	        "WHERE 1=1");

	    List<Object> amtParams = new ArrayList<>();
	    if (boyId != null) {
	        amtSql.append(" AND da.delivery_boy_id = ?");
	        amtParams.add(boyId);
	    }
	    try {
	        jdbcTemplate.queryForObject(amtSql.toString(), amtParams.toArray(), (rs, rowNum) -> {
	            if (dto != null) {
	                dto.setTodayNetValue(rs.getDouble("today_net_value"));
	                dto.setTotalNetValue(rs.getDouble("total_net_value"));
	                dto.setTodayCollected(rs.getDouble("today_collected"));
	                dto.setTotalCollected(rs.getDouble("total_collected"));
	            }
	            return null;
	        });
	    } catch (Exception e) {
	        log.warn("getDeliverySummary: amounts query failed (boyId={}) — amounts will be 0. Cause: {}",
	                 boyId, e.getMessage());
	    }
	    return dto;
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
