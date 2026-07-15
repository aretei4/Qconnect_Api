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
	            COUNT(*)                                   AS total,
	            COUNT(*) FILTER (WHERE status = 2)         AS delivered,
	            COUNT(*) FILTER (WHERE status = 0)         AS pending,
	            COUNT(*) FILTER (WHERE status = 1)         AS cancelled,

	            COUNT(*) FILTER (WHERE status IN (0, 1, 2, 9)) AS today_total,
	            COUNT(*) FILTER (WHERE status = 2)         AS today_delivered,
	            COUNT(*) FILTER (WHERE status = 0)         AS today_pending,
	            COUNT(*) FILTER (WHERE status = 1)         AS today_cancelled,
	            COUNT(*) FILTER (WHERE status = 9)         AS assigned,
	            COUNT(*) FILTER (WHERE status = 8)         AS rejected

	        FROM delivery_assignments
	        WHERE status != 10
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
	            d.setAssigned(rs.getInt("assigned"));
	            d.setRejected(rs.getInt("rejected"));
	            return d;
	        }
	    );

	    // ── Net value (from stage_sales_entery — independent, must never be zeroed by a payment-side error) ──
	    StringBuilder netSql = new StringBuilder(
	        "SELECT " +
	        "  COALESCE(SUM(CAST(sse.net_value AS double precision)), 0)                                 AS net_value, " +
	        "  COALESCE(SUM(CAST(sse.net_value AS double precision)) FILTER (WHERE da.status = 9), 0)    AS assigned_value, " +
	        "  COALESCE(SUM(CAST(sse.net_value AS double precision)) FILTER (WHERE da.status = 8), 0)    AS rejected_value " +
	        "FROM delivery_assignments da " +
	        "JOIN stage_sales_entery sse ON sse.dire_id = da.dire_id " +
	        "WHERE da.status != 10");
	    List<Object> netParams = new ArrayList<>();
	    if (boyId != null) {
	        netSql.append(" AND da.delivery_boy_id = ?");
	        netParams.add(boyId);
	    }
	    try {
	        jdbcTemplate.queryForObject(netSql.toString(), netParams.toArray(), (rs, rowNum) -> {
	            if (dto != null) {
	                dto.setTodayNetValue(rs.getDouble("net_value"));
	                dto.setTotalNetValue(rs.getDouble("net_value"));
	                dto.setAssignedValue(rs.getDouble("assigned_value"));
	                dto.setRejectedValue(rs.getDouble("rejected_value"));
	            }
	            return null;
	        });
	    } catch (Exception e) {
	        log.warn("getDeliverySummary: net value query failed (boyId={}) — net value will be 0. Cause: {}",
	                 boyId, e.getMessage());
	    }

	    // ── Collected amount (from delivery_status — separate so a failure here can't zero net value) ──
	    StringBuilder amtSql = new StringBuilder(
	        "SELECT " +
	        "  COALESCE(SUM(ds.payment_amount) FILTER (WHERE da.status = 2), 0)  AS today_collected, " +
	        "  COALESCE(SUM(ds.payment_amount) FILTER (WHERE da.status = 2), 0)  AS total_collected " +
	        "FROM delivery_assignments da " +
	        "LEFT JOIN delivery_status ds ON ds.dire_id = da.dire_id " +
	        "WHERE da.status != 10");
	    List<Object> amtParams = new ArrayList<>();
	    if (boyId != null) {
	        amtSql.append(" AND da.delivery_boy_id = ?");
	        amtParams.add(boyId);
	    }
	    try {
	        jdbcTemplate.queryForObject(amtSql.toString(), amtParams.toArray(), (rs, rowNum) -> {
	            if (dto != null) {
	                dto.setTodayCollected(rs.getDouble("today_collected"));
	                dto.setTotalCollected(rs.getDouble("total_collected"));
	            }
	            return null;
	        });
	    } catch (Exception e) {
	        log.warn("getDeliverySummary: collected query failed (boyId={}) — collected will be 0. Cause: {}",
	                 boyId, e.getMessage());
	    }
	    return dto;
	}
	
	/** Overall summary — CLOSED (status 10) records only: count, net value, collected. */
	public OverallSummaryDto getOverallSummary(String boyId) {
	    StringBuilder sql = new StringBuilder(
	        "SELECT " +
	        "  COUNT(*)                                                    AS closed_count, " +
	        "  COALESCE(SUM(CAST(sse.net_value AS double precision)), 0)   AS closed_value, " +
	        "  COALESCE(SUM(ds.payment_amount), 0)                         AS closed_collected " +
	        "FROM delivery_assignments da " +
	        "LEFT JOIN stage_sales_entery sse ON sse.dire_id = da.dire_id " +
	        "LEFT JOIN delivery_status    ds  ON ds.dire_id  = da.dire_id " +
	        "WHERE da.status = 10");

	    List<Object> params = new ArrayList<>();
	    if (boyId != null) {
	        sql.append(" AND da.delivery_boy_id = ?");
	        params.add(boyId);
	    }

	    return jdbcTemplate.queryForObject(sql.toString(), params.toArray(), (rs, rowNum) -> {
	        OverallSummaryDto d = new OverallSummaryDto();
	        d.setClosed(rs.getInt("closed_count"));
	        d.setClosedValue(rs.getDouble("closed_value"));
	        d.setClosedCollected(rs.getDouble("closed_collected"));
	        return d;
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
