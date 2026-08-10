package com.api.distr.docs.sales.repo;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

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
                                    rs.getInt("bu_id"),
                                    "DELIVERY",
                                    null   // delivery agents don't get a JWT
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

            // ── 2. Upsert delivery_status — payment columns NOT written here anymore;
            //       all payment data lives in payment_details (step 3) ─────────────
            jdbcTemplate.update("""
                    INSERT INTO delivery_status
                        (dire_id, delivery_id, delivered, otp,
                         picklist_no, reason, lat, lon, delivery_date)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, NOW())
                    ON CONFLICT (dire_id) DO UPDATE SET
                        delivered      = EXCLUDED.delivered,
                        otp            = EXCLUDED.otp,
                        reason         = EXCLUDED.reason,
                        lat            = EXCLUDED.lat,
                        lon            = EXCLUDED.lon,
                        delivery_date  = NOW()
                    """,
                    direId, d.getDelivery_id(), d.isDelivered(), d.isOtp(),
                    picklistNo, d.getReason(), d.getLat(), d.getLon());

            // ── 3. Payment details — the single source of truth for payments ──────
            upsertPaymentDetails(direId, d.getPaymentModes());

            log.info("upsertByDireId: completed for direId={}, picklistNo={}", direId, picklistNo);
        } catch (Exception e) {
            log.error("upsertByDireId failed: direId={}, error={}", direId, e.getMessage(), e);
            throw e;
        }
    }

	/**
	 * Replaces the payment_details row for a dire_id.
	 * Single row per transaction — mode amounts flattened into dedicated columns
	 * (cash_amount, upi_amount, cheque_amount, neft_amount) with their refs.
	 */
	public void upsertPaymentDetails(Long direId,
	        List<com.api.distr.docs.sales.dto.PaymentModeEntry> modes) {
        try {
            // Replace previous row for this transaction
            jdbcTemplate.update("DELETE FROM payment_details WHERE dire_id = ?", direId);

            if (modes == null || modes.isEmpty()) {
                log.info("upsertPaymentDetails: no payment modes for direId={}", direId);
                return;
            }

            double cashAmt = 0, upiAmt = 0, chequeAmt = 0, neftAmt = 0, creditAmt = 0;
            String upiRef = null, chequeNo = null, chequeBank = null, neftRef = null;

            for (com.api.distr.docs.sales.dto.PaymentModeEntry m : modes) {
                String mode = m.getMode() != null ? m.getMode().toUpperCase() : "";
                switch (mode) {
                    case "CASH" -> cashAmt += m.getAmount();
                    case "UPI" -> {
                        upiAmt += m.getAmount();
                        if (m.getReferenceNo() != null) upiRef = m.getReferenceNo();
                    }
                    case "CHEQUE" -> {
                        chequeAmt += m.getAmount();
                        if (m.getChequeNo() != null) chequeNo   = m.getChequeNo();
                        if (m.getBankName() != null) chequeBank = m.getBankName();
                    }
                    case "NEFT", "BANK_TRANSFER" -> {
                        neftAmt += m.getAmount();
                        if (m.getReferenceNo() != null) neftRef = m.getReferenceNo();
                    }
                    case "CREDIT" -> creditAmt += m.getAmount();
                    default -> cashAmt += m.getAmount(); // unknown modes counted as cash
                }
            }
            double total = cashAmt + upiAmt + chequeAmt + neftAmt + creditAmt;

            // return_amount = invoice net_value − total paid (0 when fully paid or net_value missing)
            Double netValue = 0.0;
            try {
                netValue = jdbcTemplate.queryForObject(
                        "SELECT COALESCE(CAST(net_value AS double precision), 0) FROM stage_sales_entery WHERE dire_id = ?",
                        Double.class, direId);
            } catch (Exception ignored) {}
            double returnAmt = Math.max(0, (netValue != null ? netValue : 0.0) - total);

            jdbcTemplate.update("""
                    INSERT INTO payment_details
                        (dire_id, total_amount, return_amount, net_amount,
                         cash_amount,
                         upi_amount, upi_ref_no,
                         cheque_amount, cheque_no, cheque_bank,
                         neft_amount, neft_ref_no,
                         credit_amount,
                         payment_status, payment_date, created_at)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_DATE, NOW())
                    """,
                    direId, total, returnAmt, total,
                    cashAmt,
                    upiAmt, upiRef,
                    chequeAmt, chequeNo, chequeBank,
                    neftAmt, neftRef,
                    creditAmt,
                    total > 0 ? "paid" : "pending");

            log.info("upsertPaymentDetails: direId={}, total={}, return={}, cash={}, upi={}, cheque={}, neft={}, credit={}",
                    direId, total, returnAmt, cashAmt, upiAmt, chequeAmt, neftAmt, creditAmt);
        } catch (Exception e) {
            // Payment details are supplementary — never fail the main delivery update
            log.error("upsertPaymentDetails failed: direId={}, error={}", direId, e.getMessage(), e);
        }
    }

	public void saveOrUpdate(DeliveryRequest request) {
        List<Long> direIds = request.getDireIds();
        log.info("saveOrUpdate: deliveryBoyId={}, direIdCount={}", request.getDeliveryBoyId(),
                direIds != null ? direIds.size() : 0);
        if (direIds == null || direIds.isEmpty())
            throw new IllegalArgumentException("direIds is required for assignment");
        try {
            String sql = """
                        INSERT INTO delivery_assignments
                        (dire_id, delivery_boy_id, car_no, driver_name, mobile, updated_at, status)
                        VALUES (?, ?, ?, ?, ?, ?, 9)
                        ON CONFLICT (dire_id)
                        DO UPDATE SET
                            delivery_boy_id = EXCLUDED.delivery_boy_id,
                            car_no          = EXCLUDED.car_no,
                            driver_name     = EXCLUDED.driver_name,
                            mobile          = EXCLUDED.mobile,
                            updated_at      = EXCLUDED.updated_at,
                            status          = 9
                    """;

            for (Long direId : direIds) {
                jdbcTemplate.update(sql,
                        direId,
                        request.getDeliveryBoyId(),
                        request.getCar().getCarNo(),
                        request.getCar().getDriverName(),
                        request.getCar().getMobile(),
                        LocalDateTime.now());
            }
            log.info("saveOrUpdate: completed {} assignments for deliveryBoyId={}", direIds.size(), request.getDeliveryBoyId());
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
	 * Saves the route order for already-assigned stops.
	 * Only updates sequence (and updated_at) — assignment, status, and coordinates untouched.
	 */
	public void saveSmartRouteAssignments(java.util.List<SmartRouteAssignItem> items) {
        log.info("saveSmartRouteAssignments: {} stops", items.size());
        try {
            String sql = """
                    UPDATE delivery_assignments
                    SET    sequence   = ?,
                           updated_at = ?
                    WHERE  dire_id = ?
                    """;
            int updated = 0;
            for (SmartRouteAssignItem item : items) {
                if (item.direId == null) {
                    log.warn("saveSmartRouteAssignments: skipping stop without direId (sequence={})", item.sequence);
                    continue;
                }
                updated += jdbcTemplate.update(sql,
                        item.sequence,
                        LocalDateTime.now(),
                        item.direId);
            }
            log.info("saveSmartRouteAssignments: sequence updated for {} of {} stops", updated, items.size());
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

	/** Accept newly-assigned deliveries (status 9 → 0 PENDING). */
	public int acceptAssignments(List<Long> direIds) {
		log.info("acceptAssignments: direIds={}", direIds);
		int rows = 0;
		for (Long direId : direIds) {
			rows += jdbcTemplate.update(
				"UPDATE delivery_assignments SET status = 0, updated_at = NOW() WHERE dire_id = ? AND status = 9",
				direId);
		}
		log.info("acceptAssignments: updated {} of {} row(s)", rows, direIds.size());
		return rows;
	}

	/** Reject newly-assigned deliveries (status 9 → 8 REJECTED). */
	public int rejectAssignments(List<Long> direIds) {
		log.info("rejectAssignments: direIds={}", direIds);
		int rows = 0;
		for (Long direId : direIds) {
			rows += jdbcTemplate.update(
				"UPDATE delivery_assignments SET status = 8, updated_at = NOW() WHERE dire_id = ? AND status = 9",
				direId);
		}
		log.info("rejectAssignments: updated {} of {} row(s)", rows, direIds.size());
		return rows;
	}

    public List<SalesEntryDto> getAllSalesByStatus(String deliveryId, String statusFilter) {
        String statusCondition = switch (statusFilter) {
            case "delivered" -> "da.status = 2";
            case "cancelled" -> "da.status = 1";
            case "all"       -> "da.status IN (0, 1, 2, 9)";
            case "new"       -> "da.status = 9";
            default          -> "da.status IN (0, 9)";
        };
        String sql = QueryConstants.SELECT_DELIVERY_ASSIGN_BASE + statusCondition + " AND da.status != 10";
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

	/** Open DANs blocking new dispatch — STARTED, PENDING or awaiting the accounts desk. */
	public Integer countPendingDans() {
		return jdbcTemplate.queryForObject(
			"SELECT COUNT(*) FROM dayend_approval WHERE status IN (" +
			com.api.distr.docs.dayend.DayEndStatus.STARTED + "," +
			com.api.distr.docs.dayend.DayEndStatus.PENDING + "," +
			com.api.distr.docs.dayend.DayEndStatus.SK_APPROVED + ")",
			Integer.class);
	}

	public List<com.api.distr.docs.sales.dto.AssignmentDTO> getAllAssignments(
            String fromDate, String toDate, String agentId, Integer status) {

        StringBuilder sql = new StringBuilder("""
            SELECT
                sse.dire_id                              AS direId,
                sse.sales_order_no                       AS invoiceNo,
                sse.customer_no                          AS customerNo,
                sse.cust_desc                            AS custDesc,
                cd.cust_mobile                           AS custMobile,
                sse.net_value::text                      AS netValue,
                TO_CHAR(sse.billing_date, 'DD/MM/YYYY')  AS billingDate,
                TO_CHAR(da.delivery_date, 'DD/MM/YYYY')  AS deliveryDate,
                da.delivery_boy_id                       AS agentId,
                COALESCE(dm.delivery_name, 'Unknown')    AS agentName,
                da.status                                AS assignStatus,
                CASE da.status
                    WHEN 0  THEN 'PENDING'
                    WHEN 9  THEN 'ASSIGNED'
                    WHEN 8  THEN 'REJECTED'
                    WHEN 2  THEN 'DELIVERED'
                    WHEN 10 THEN 'CLOSED'
                    ELSE 'FAILED'
                END                                      AS statusLabel
            FROM delivery_assignments da
            JOIN stage_sales_entery sse ON sse.dire_id = da.dire_id
            LEFT JOIN customer_details cd ON cd.cust_no = sse.customer_no
            LEFT JOIN delivery_master dm ON dm.delivery_id::text = da.delivery_boy_id
            WHERE 1=1
            """);

        java.util.List<Object> params = new java.util.ArrayList<>();

        if (fromDate != null && !fromDate.isBlank()) {
            sql.append(" AND sse.billing_date >= TO_DATE(?, 'DD/MM/YYYY')");
            params.add(fromDate);
        }
        if (toDate != null && !toDate.isBlank()) {
            sql.append(" AND sse.billing_date <= TO_DATE(?, 'DD/MM/YYYY')");
            params.add(toDate);
        }
        if (agentId != null && !agentId.isBlank()) {
            sql.append(" AND da.delivery_boy_id = ?");
            params.add(agentId);
        }
        sql.append(" AND da.status != 10");
        if (status != null) {
            sql.append(" AND da.status = ?");
            params.add(status);
        }
        sql.append(" ORDER BY da.updated_at DESC NULLS LAST, dm.delivery_name");

        log.info("getAllAssignments: fromDate={}, toDate={}, agentId={}, status={}", fromDate, toDate, agentId, status);
        try {
            List<com.api.distr.docs.sales.dto.AssignmentDTO> result =
                jdbcTemplate.query(sql.toString(), params.toArray(), (rs, rowNum) -> {
                    com.api.distr.docs.sales.dto.AssignmentDTO dto = new com.api.distr.docs.sales.dto.AssignmentDTO();
                    dto.setDireId(rs.getLong("direId"));
                    dto.setInvoiceNo(rs.getString("invoiceNo"));
                    dto.setCustomerNo(rs.getString("customerNo"));
                    dto.setCustDesc(rs.getString("custDesc"));
                    dto.setCustMobile(rs.getString("custMobile"));
                    dto.setNetValue(rs.getString("netValue"));
                    dto.setBillingDate(rs.getString("billingDate"));
                    dto.setDeliveryDate(rs.getString("deliveryDate"));
                    dto.setAgentId(rs.getString("agentId"));
                    dto.setAgentName(rs.getString("agentName"));
                    dto.setAssignStatus(rs.getInt("assignStatus"));
                    dto.setStatusLabel(rs.getString("statusLabel"));
                    return dto;
                });
            log.info("getAllAssignments: returned {} rows", result.size());
            return result;
        } catch (Exception e) {
            log.error("getAllAssignments failed: {}", e.getMessage(), e);
            throw e;
        }
    }

	public List<SalesEntryDto> getAllSales(String deliveryId) {
        String sql = QueryConstants.SELECT_DELIVERY_ASSIGN + deliveryId + "'" +
            " AND da.status != 10";
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
