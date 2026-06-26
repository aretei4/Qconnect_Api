package com.api.distr.docs.sales.repo;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import com.api.distr.docs.sales.dto.DeliveryAgent;
import com.api.distr.docs.sales.dto.DeliveryLoginResponse;
import com.api.distr.docs.sales.dto.DeliveryRequest;
import com.api.distr.docs.sales.dto.DeliveryStatus;
import com.api.distr.docs.sales.dto.SalesEntryDto;
import com.api.distr.docs.sales.dto.SmartRouteAssignItem;

@Repository
public class DeliveryRepository {

	private static final Logger log = LoggerFactory.getLogger(DeliveryRepository.class);

	private final JdbcTemplate jdbcTemplate;

	public DeliveryRepository(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	/**
	 * Ensures delivery_status has all required columns and the correct constraints.
	 * Safe to run on every startup — all DDL operations are idempotent.
	 */
	@PostConstruct
	public void ensureSchema() {
		// Drop the old picklist_no unique constraint — upsert now keys on dire_id
		try {
			jdbcTemplate.execute(
				"ALTER TABLE delivery_status DROP CONSTRAINT IF EXISTS uq_delivery_status_picklist_no");
			log.info("ensureSchema: dropped uq_delivery_status_picklist_no OK");
		} catch (Exception e) {
			log.warn("ensureSchema: could not drop uq_delivery_status_picklist_no — {}", e.getMessage());
		}
		try {
			jdbcTemplate.execute(
				"ALTER TABLE delivery_status ADD COLUMN IF NOT EXISTS dire_id BIGINT");
			log.info("ensureSchema: delivery_status.dire_id OK");
		} catch (Exception e) {
			log.warn("ensureSchema: could not add delivery_status.dire_id — {}", e.getMessage());
		}
		try {
			jdbcTemplate.execute(
				"ALTER TABLE delivery_status ADD COLUMN IF NOT EXISTS delivery_date TIMESTAMP");
			log.info("ensureSchema: delivery_status.delivery_date OK");
		} catch (Exception e) {
			log.warn("ensureSchema: could not add delivery_status.delivery_date — {}", e.getMessage());
		}
		try {
			jdbcTemplate.execute(
				"ALTER TABLE delivery_status ADD COLUMN IF NOT EXISTS payment_amount NUMERIC(12,2) DEFAULT 0");
			log.info("ensureSchema: delivery_status.payment_amount OK");
		} catch (Exception e) {
			log.warn("ensureSchema: could not add delivery_status.payment_amount — {}", e.getMessage());
		}
		try {
			jdbcTemplate.execute(
				"ALTER TABLE delivery_status ADD COLUMN IF NOT EXISTS delivered BOOLEAN DEFAULT false");
			log.info("ensureSchema: delivery_status.delivered OK");
		} catch (Exception e) {
			log.warn("ensureSchema: could not add delivery_status.delivered — {}", e.getMessage());
		}
		try {
			jdbcTemplate.execute("""
				DO $$
				BEGIN
				    IF NOT EXISTS (
				        SELECT 1 FROM pg_constraint WHERE conname = 'uq_delivery_assignments_dire_id'
				    ) THEN
				        ALTER TABLE delivery_assignments ADD CONSTRAINT uq_delivery_assignments_dire_id UNIQUE (dire_id);
				    END IF;
				END$$
				""");
			log.info("ensureSchema: delivery_assignments UNIQUE(dire_id) OK");
		} catch (Exception e) {
			log.warn("ensureSchema: UNIQUE(dire_id) on delivery_assignments skipped — {}", e.getMessage());
		}
		// UNIQUE on delivery_status.dire_id — required for ON CONFLICT (dire_id) upsert
		try {
			jdbcTemplate.execute("""
				DO $$
				BEGIN
				    IF NOT EXISTS (
				        SELECT 1 FROM pg_constraint WHERE conname = 'uq_delivery_status_dire_id'
				    ) THEN
				        ALTER TABLE delivery_status ADD CONSTRAINT uq_delivery_status_dire_id UNIQUE (dire_id);
				    END IF;
				END$$
				""");
			log.info("ensureSchema: delivery_status UNIQUE(dire_id) OK");
		} catch (Exception e) {
			log.warn("ensureSchema: UNIQUE(dire_id) on delivery_status skipped — {}", e.getMessage());
		}
	}

	public DeliveryLoginResponse findByMobile(String mobile) {
        log.info("findByMobile: mobile={}", mobile);
        try {
            return jdbcTemplate.queryForObject(
                    QueryConstants.SELECT_DELIVERY_MOBILE,
                    new Object[]{mobile},
                    (rs, rowNum) ->
                            new DeliveryLoginResponse(
                                    rs.getLong("delivery_id"),
                                    rs.getString("delivery_name"),
                                    rs.getInt("bu_id")
                            )
            );
        } catch (Exception e) {
            log.error("findByMobile failed: mobile={}, error={}", mobile, e.getMessage(), e);
            throw e;
        }
    }
	
	public void upsertByDireId(DeliveryStatus d) {
        Long direId = d.getDireId();
        if (direId == null || direId <= 0) {
            throw new IllegalArgumentException("dire_id is required for delivery-status update");
        }
        log.info("upsertByDireId: direId={}, delivered={}, deliveryId={}",
                direId, d.isDelivered(), d.getDelivery_id());
        try {
            // Look up picklist_no from source table — still stored for DayEnd / reporting queries
            String picklistNo = null;
            try {
                picklistNo = jdbcTemplate.queryForObject(
                        "SELECT picklist_no FROM stage_sales_entery WHERE dire_id = ?",
                        String.class, direId);
            } catch (Exception ignored) {}

            // ── 1. Update delivery_assignments ────────────────────────────────────
            jdbcTemplate.update("""
                    UPDATE delivery_assignments
                    SET    status        = ?,
                           delivery_date = CURRENT_TIMESTAMP
                    WHERE  dire_id       = ?
                    """, d.isDelivered() ? 2 : 1, direId);

            // ── 2. Upsert delivery_status — conflict on dire_id ───────────────────
            jdbcTemplate.update("""
                    INSERT INTO delivery_status
                        (dire_id, delivery_id, delivered, otp, payment_amount, payment_mode,
                         picklist_no, reason, lat, lon, delivery_date)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW())
                    ON CONFLICT (dire_id) DO UPDATE SET
                        delivered      = EXCLUDED.delivered,
                        otp            = EXCLUDED.otp,
                        payment_amount = EXCLUDED.payment_amount,
                        payment_mode   = EXCLUDED.payment_mode,
                        reason         = EXCLUDED.reason,
                        lat            = EXCLUDED.lat,
                        lon            = EXCLUDED.lon,
                        delivery_date  = NOW()
                    """,
                    direId, d.getDelivery_id(), d.isDelivered(), d.isOtp(),
                    d.getTotalPaymentAmount(), d.getPaymentModeDbValue(),
                    picklistNo, d.getReason(), d.getLat(), d.getLon());

            log.info("upsertByDireId: completed for direId={}, picklistNo={}", direId, picklistNo);
        } catch (Exception e) {
            log.error("upsertByDireId failed: direId={}, error={}", direId, e.getMessage(), e);
            throw e;
        }
    }

	public void saveOrUpdate(DeliveryRequest request) {
        log.info("saveOrUpdate: deliveryBoyId={}, picklistCount={}",
                request.getDeliveryBoyId(),
                request.getPicklistNos() != null ? request.getPicklistNos().size() : 0);
        try {
            List<String> picklistNos = request.getPicklistNos();
            List<Long>   direIds     = request.getDireIds();
            boolean hasDireIds = direIds != null && direIds.size() == picklistNos.size();

            String sqlWithDireId = """
                        INSERT INTO delivery_assignments
                        (dire_id, delivery_boy_id, car_no, driver_name, mobile, updated_at)
                        VALUES (?, ?, ?, ?, ?, ?)
                        ON CONFLICT (dire_id)
                        DO UPDATE SET
                            delivery_boy_id = EXCLUDED.delivery_boy_id,
                            car_no          = EXCLUDED.car_no,
                            driver_name     = EXCLUDED.driver_name,
                            mobile          = EXCLUDED.mobile,
                            updated_at      = EXCLUDED.updated_at
                    """;

            String sqlLookupDireId = """
                        INSERT INTO delivery_assignments
                        (dire_id, delivery_boy_id, car_no, driver_name, mobile, updated_at)
                        VALUES (
                            (SELECT dire_id FROM stage_sales_entery WHERE picklist_no = ? LIMIT 1),
                            ?, ?, ?, ?, ?
                        )
                        ON CONFLICT (dire_id)
                        DO UPDATE SET
                            delivery_boy_id = EXCLUDED.delivery_boy_id,
                            car_no          = EXCLUDED.car_no,
                            driver_name     = EXCLUDED.driver_name,
                            mobile          = EXCLUDED.mobile,
                            updated_at      = EXCLUDED.updated_at
                    """;

            for (int i = 0; i < picklistNos.size(); i++) {
                String picklistNo = picklistNos.get(i);
                if (hasDireIds) {
                    jdbcTemplate.update(sqlWithDireId,
                            direIds.get(i),
                            request.getDeliveryBoyId(),
                            request.getCar().getCarNo(),
                            request.getCar().getDriverName(),
                            request.getCar().getMobile(),
                            LocalDateTime.now());
                } else {
                    jdbcTemplate.update(sqlLookupDireId,
                            picklistNo,
                            request.getDeliveryBoyId(),
                            request.getCar().getCarNo(),
                            request.getCar().getDriverName(),
                            request.getCar().getMobile(),
                            LocalDateTime.now());
                }
            }
            log.info("saveOrUpdate: completed for deliveryBoyId={}", request.getDeliveryBoyId());
        } catch (Exception e) {
            log.error("saveOrUpdate failed: deliveryBoyId={}, error={}", request.getDeliveryBoyId(), e.getMessage(), e);
            throw e;
        }
    }

	private static class DeliveryMapper implements RowMapper<DeliveryAgent> {
		@Override
		public DeliveryAgent mapRow(ResultSet rs, int rowNum) throws SQLException {
			DeliveryAgent d = new DeliveryAgent();
			d.setId     (rs.getLong("delivery_id"));
			d.setName   (rs.getString("delivery_name"));
			d.setContact(rs.getString("delivery_mobile"));
			d.setAltContact(rs.getString("alt_mobile"));
			d.setActive (rs.getBoolean("active"));
			d.setBuId   (rs.getInt("bu_id"));
			d.setUpdatedDate(rs.getDate("updated_date") != null
				? rs.getDate("updated_date").toLocalDate() : null);

			// ── Address ───────────────────────────────────────────────────────
			d.setAddress1(rs.getString("address1"));
			d.setAddress2(rs.getString("address2"));
			d.setAddress3(rs.getString("address3"));
			d.setCity    (rs.getString("city"));
			d.setPinCode (rs.getString("pin_code"));

			// ── Identity & Banking ────────────────────────────────────────────
			d.setFatherName (rs.getString("father_name"));
			d.setAadharNo   (rs.getString("aadhar_no"));
			d.setPanCard    (rs.getString("pan_card"));
			d.setBankAccount(rs.getString("bank_account"));
			d.setDateOfJoining(rs.getDate("date_of_joining") != null
				? rs.getDate("date_of_joining").toLocalDate() : null);

			return d;
		}
	}

	public List<DeliveryAgent> findAll() {
		String sql = "SELECT * FROM delivery_master ORDER BY delivery_id";
		return jdbcTemplate.query(sql, new DeliveryMapper());
	}

	/** Insert a new agent from the Add Agent form (frontend). */
	public DeliveryAgent createAgent(DeliveryAgent req) {
		String sql = """
			INSERT INTO delivery_master (
				delivery_name, delivery_mobile, alt_mobile,
				active, bu_id,
				address1, address2, address3, city, pin_code,
				father_name, aadhar_no, pan_card, bank_account,
				date_of_joining, updated_date
			) VALUES (
				?, ?, ?,
				true, 100,
				?, ?, ?, ?, ?,
				?, ?, ?, ?,
				?, CURRENT_DATE
			) RETURNING delivery_id
			""";

		Long newId = jdbcTemplate.queryForObject(sql, Long.class,
			req.getName(),
			req.getContact(),
			req.getAltContact(),
			req.getAddress1(),
			req.getAddress2(),
			req.getAddress3(),
			req.getCity(),
			req.getPinCode(),
			req.getFatherName(),
			req.getAadharNo(),
			req.getPanCard(),
			req.getBankAccount(),
			req.getDateOfJoining() != null
				? java.sql.Date.valueOf(req.getDateOfJoining()) : null
		);

		req.setId(newId);
		req.setActive(true);
		req.setBuId(100);
		return req;
	}

	public DeliveryAgent updateAgent(Long id, DeliveryAgent req) {
		String sql = """
				UPDATE delivery_master SET
				    delivery_name   = ?,
				    delivery_mobile = ?,
				    alt_mobile      = ?,
				    address1        = ?,
				    address2        = ?,
				    address3        = ?,
				    city            = ?,
				    pin_code        = ?,
				    father_name     = ?,
				    aadhar_no       = ?,
				    pan_card        = ?,
				    bank_account    = ?,
				    date_of_joining = ?,
				    updated_date    = CURRENT_DATE
				WHERE delivery_id = ?
				""";
		int rows = jdbcTemplate.update(sql,
				req.getName(),
				req.getContact(),
				req.getAltContact(),
				req.getAddress1(),
				req.getAddress2(),
				req.getAddress3(),
				req.getCity(),
				req.getPinCode(),
				req.getFatherName(),
				req.getAadharNo(),
				req.getPanCard() != null ? req.getPanCard().toUpperCase() : null,
				req.getBankAccount(),
				req.getDateOfJoining() != null ? java.sql.Date.valueOf(req.getDateOfJoining()) : null,
				id);
		if (rows == 0) throw new IllegalArgumentException("Agent not found: " + id);
		req.setId(id);
		return req;
	}

	/**
	 * Upsert a full Smart Route assignment list.
	 * Writes sequence, lat, lon, address, delivery_boy_id per stop.
	 * Sets status = 0 (PENDING) and delivery_date = today.
	 */
	public void saveSmartRouteAssignments(java.util.List<SmartRouteAssignItem> items) {
        log.info("saveSmartRouteAssignments: {} stops", items.size());
        try {
            String sql = """
                    INSERT INTO delivery_assignments
                        (dire_id, delivery_boy_id, sequence, lat, lon, address, delivery_date, updated_at, status)
                    VALUES (
                        (SELECT dire_id FROM stage_sales_entery WHERE picklist_no = ? LIMIT 1),
                        ?, ?, ?, ?, ?, CURRENT_DATE, ?, 0
                    )
                    ON CONFLICT (dire_id)
                    DO UPDATE SET
                        delivery_boy_id = EXCLUDED.delivery_boy_id,
                        sequence        = EXCLUDED.sequence,
                        lat             = EXCLUDED.lat,
                        lon             = EXCLUDED.lon,
                        address         = EXCLUDED.address,
                        delivery_date   = EXCLUDED.delivery_date,
                        updated_at      = EXCLUDED.updated_at,
                        status          = 0
                    """;
            for (SmartRouteAssignItem item : items) {
                jdbcTemplate.update(sql,
                        item.picklist_no,
                        item.deliveryBoyId,
                        item.sequence,
                        item.lat     != null ? item.lat     : 0.0,
                        item.lon     != null ? item.lon     : 0.0,
                        item.address != null ? item.address : "",
                        LocalDateTime.now());
            }
            log.info("saveSmartRouteAssignments: completed {} stops", items.size());
        } catch (Exception e) {
            log.error("saveSmartRouteAssignments failed: stopCount={}, error={}", items.size(), e.getMessage(), e);
            throw e;
        }
    }

	public int deleteByPicklistNo(String picklistNo) {
		return jdbcTemplate.update(
			"DELETE FROM delivery_assignments da " +
			"USING stage_sales_entery sse " +
			"WHERE sse.picklist_no = ? AND da.dire_id = sse.dire_id",
			picklistNo);
	}

	/** Delete delivery_assignments row by dire_id. */
	public int deleteByDireId(Long direId) {
		try {
			return jdbcTemplate.update(
				"DELETE FROM delivery_assignments WHERE dire_id = ?", direId);
		} catch (Exception e) {
			log.error("deleteByDireId failed: direId={}, error={}", direId, e.getMessage(), e);
			throw e;
		}
	}

    public List<SalesEntryDto> getAllSalesByStatus(String deliveryId, String statusFilter) {
        String statusCondition = switch (statusFilter) {
            case "delivered" -> "da.status = 2";
            case "cancelled" -> "da.status = 1";
            case "all"       -> "da.status IN (0, 1, 2)";
            default          -> "da.status = 0";
        };
        String danNotClosedCondition =
            " AND NOT EXISTS (" +
            "   SELECT 1 FROM dayend_approval dap" +
            "   WHERE dap.delivery_id::text = da.delivery_boy_id" +
            "     AND dap.status = 'CLOSED'" +
            ")";
        String sql = QueryConstants.SELECT_DELIVERY_ASSIGN_BASE + statusCondition + danNotClosedCondition;
        log.info("getAllSalesByStatus: deliveryId={}, filter={}", deliveryId, statusFilter);
        try {
            List<SalesEntryDto> result = jdbcTemplate.query(sql, new Object[]{deliveryId}, (rs, rowNum) -> {
                SalesEntryDto dto = new SalesEntryDto();
                dto.setDireId(rs.getLong("direId"));
                dto.setInvoiceNo(rs.getString("invoiceNo"));
                dto.setPicklistNo(rs.getString("picklistNo"));
                dto.setCustomerNo(rs.getString("customerNo"));
                dto.setCustDesc(rs.getString("custDesc"));
                dto.setCustMobile(rs.getString("custMobile"));
                dto.setNetValue("" + rs.getDouble("netValue"));
                dto.setAssignStatus(rs.getInt("assignStatus"));
                return dto;
            });
            log.info("getAllSalesByStatus: returned {} entries", result.size());
            return result;
        } catch (Exception e) {
            log.error("getAllSalesByStatus failed: {}", e.getMessage(), e);
            throw e;
        }
    }

	public Integer countPendingDans() {
		return jdbcTemplate.queryForObject(
			"SELECT COUNT(*) FROM dayend_approval WHERE status IN ('STARTED','PENDING')",
			Integer.class);
	}

	public List<SalesEntryDto> getAllSales(String deliveryId) {
        String sql = QueryConstants.SELECT_DELIVERY_ASSIGN + deliveryId + "'" +
            " AND NOT EXISTS (" +
            "   SELECT 1 FROM dayend_approval dap" +
            "   WHERE dap.delivery_id::text = da.delivery_boy_id" +
            "     AND dap.status = 'CLOSED'" +
            ")";
        log.info("getAllSales: deliveryId={}", deliveryId);
        try {
            List<SalesEntryDto> result = jdbcTemplate.query(sql, (rs, rowNum) -> {
                SalesEntryDto dto = new SalesEntryDto();
                dto.setDireId(rs.getLong("direId"));
                dto.setInvoiceNo(rs.getString("invoiceNo"));
                dto.setPicklistNo(rs.getString("picklistNo"));
                dto.setCustomerNo(rs.getString("customerNo"));
                dto.setCustDesc(rs.getString("custDesc"));
                dto.setCustMobile(rs.getString("custMobile"));
                dto.setNetValue("" + rs.getDouble("netValue"));
                dto.setAssignStatus(rs.getInt("assignStatus"));
                return dto;
            });
            log.info("getAllSales: returned {} entries for deliveryId={}", result.size(), deliveryId);
            return result;
        } catch (Exception e) {
            log.error("getAllSales failed: deliveryId={}, error={}", deliveryId, e.getMessage(), e);
            throw e;
        }
    }

}
