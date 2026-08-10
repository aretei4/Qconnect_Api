package com.api.distr.docs.sales.repo;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import com.api.distr.docs.sales.dto.DeliveryStatusDTO;

@Service
public class DeliveryStatusService {

	private static final Logger log = LoggerFactory.getLogger(DeliveryStatusService.class);

	@Autowired
	private JdbcTemplate jdbcTemplate;

	/**
	 * Invoice Report — CLOSED (status 10) assignments only, same row shape as statusList.
	 * Payment info comes from payment_details (one row per dire_id, mode amounts in columns).
	 */
	public List<DeliveryStatusDTO> getClosedByDate(LocalDate fromDate, LocalDate toDate, String paymentMode) {
		log.info("getClosedByDate: fromDate={}, toDate={}, paymentMode={}", fromDate, toDate, paymentMode);
		try {
			// Whitelisted mode → payment_details column (prevents SQL injection)
			String modeCondition = switch (paymentMode != null ? paymentMode.toUpperCase() : "") {
				case "CASH"   -> " AND COALESCE(pd.cash_amount, 0)   > 0";
				case "UPI"    -> " AND COALESCE(pd.upi_amount, 0)    > 0";
				case "CHEQUE" -> " AND COALESCE(pd.cheque_amount, 0) > 0";
				case "NEFT"   -> " AND COALESCE(pd.neft_amount, 0)   > 0";
				case "CREDIT" -> " AND COALESCE(pd.credit_amount, 0) > 0";
				default       -> "";
			};

			String sql = """
				    SELECT
				        da.delivery_boy_id                             AS delivery_id,
				        dm.delivery_name,
				        da.dire_id,
				        COALESCE(sse.picklist_no, '')                  AS picklist_no,
				        COALESCE(sse.sales_order_no, '')               AS invoice_no,
				        COALESCE(sse.cust_desc, '')                    AS cust_desc,
				        COALESCE(CAST(sse.net_value AS double precision), 0) AS net_value,
				        'CLOSED'                                       AS status,
				        COALESCE(ds.otp, false)                        AS otp,
				        ds.reason,
				        COALESCE(pd.total_amount, 0)                   AS payment_amount,
				        COALESCE(pd.cash_amount, 0)                    AS cash_amount,
				        COALESCE(pd.upi_amount, 0)                     AS upi_amount,
				        pd.upi_ref_no,
				        COALESCE(pd.cheque_amount, 0)                  AS cheque_amount,
				        pd.cheque_no,
				        pd.cheque_bank,
				        COALESCE(pd.neft_amount, 0)                    AS neft_amount,
				        pd.neft_ref_no,
				        COALESCE(pd.credit_amount, 0)                  AS credit_amount,
				        COALESCE(da.delivery_date, da.updated_at)      AS record_date
				    FROM delivery_assignments da
				    LEFT JOIN payment_details pd  ON pd.dire_id = da.dire_id
				    LEFT JOIN delivery_status ds  ON ds.dire_id = da.dire_id
				    LEFT JOIN delivery_master dm  ON dm.delivery_id::text = da.delivery_boy_id
				    LEFT JOIN stage_sales_entery sse ON sse.dire_id = da.dire_id
				    WHERE da.status = 10
				      AND COALESCE(da.delivery_date, da.updated_at) BETWEEN ? AND ?
				""" + modeCondition + " ORDER BY record_date DESC";

			LocalDateTime from = fromDate.atStartOfDay();
			LocalDateTime to   = toDate.atTime(23, 59, 59);

			List<DeliveryStatusDTO> result = jdbcTemplate.query(sql, new Object[]{from, to}, (rs, rowNum) -> {
				DeliveryStatusDTO dto = new DeliveryStatusDTO();
				dto.delivery_id     = rs.getString("delivery_id");
				dto.deliveryBoyName = rs.getString("delivery_name");
				dto.direId          = rs.getLong("dire_id");
				dto.picklist_no     = rs.getString("picklist_no");
				dto.invoiceNo       = rs.getString("invoice_no");
				dto.custDesc        = rs.getString("cust_desc");
				dto.netValue        = rs.getDouble("net_value");
				dto.status          = rs.getString("status");
				dto.otp             = rs.getBoolean("otp");
				dto.payment_amount  = rs.getDouble("payment_amount");
				dto.reason          = rs.getString("reason");

				// Build payment modes from the payment_details columns
				List<com.api.distr.docs.sales.dto.PaymentModeEntry> modes = new java.util.ArrayList<>();
				if (rs.getDouble("cash_amount") > 0) {
					modes.add(new com.api.distr.docs.sales.dto.PaymentModeEntry("CASH", rs.getDouble("cash_amount")));
				}
				if (rs.getDouble("upi_amount") > 0) {
					var m = new com.api.distr.docs.sales.dto.PaymentModeEntry("UPI", rs.getDouble("upi_amount"));
					m.setReferenceNo(rs.getString("upi_ref_no"));
					modes.add(m);
				}
				if (rs.getDouble("cheque_amount") > 0) {
					var m = new com.api.distr.docs.sales.dto.PaymentModeEntry("CHEQUE", rs.getDouble("cheque_amount"));
					m.setChequeNo(rs.getString("cheque_no"));
					m.setBankName(rs.getString("cheque_bank"));
					modes.add(m);
				}
				if (rs.getDouble("neft_amount") > 0) {
					var m = new com.api.distr.docs.sales.dto.PaymentModeEntry("NEFT", rs.getDouble("neft_amount"));
					m.setReferenceNo(rs.getString("neft_ref_no"));
					modes.add(m);
				}
				if (rs.getDouble("credit_amount") > 0) {
					modes.add(new com.api.distr.docs.sales.dto.PaymentModeEntry("CREDIT", rs.getDouble("credit_amount")));
				}
				dto.paymentModes = modes;

				LocalDateTime date = rs.getTimestamp("record_date") != null
						? rs.getTimestamp("record_date").toLocalDateTime() : null;
				dto.delivery_date = date != null ? date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "";
				return dto;
			});
			log.info("getClosedByDate: returned {} records", result.size());
			return result;
		} catch (Exception e) {
			log.error("getClosedByDate failed: fromDate={}, toDate={}, error={}", fromDate, toDate, e.getMessage(), e);
			throw e;
		}
	}

	public List<DeliveryStatusDTO> getByUpdateDate(LocalDate fromDate, LocalDate toDate) {
		log.info("getByUpdateDate: fromDate={}, toDate={}", fromDate, toDate);
		try {

		// delivery_assignments.status: 0=PENDING  1=FAILED  2=DELIVERED
		// delivery_assignments.delivery_date: set by UPDATE when boy submits (NULL while pending)
		// delivery_assignments.delivery_boy_id: VARCHAR → cast to text for JOIN with BIGINT delivery_master.delivery_id
		// delivery_status has no date column — never reference ds.updadate_date
		String sql = """
				    SELECT
				        da.delivery_boy_id                             AS delivery_id,
				        dm.delivery_name,
				        da.dire_id,
				        COALESCE(sse.picklist_no, '')                  AS picklist_no,
				        COALESCE(sse.sales_order_no, '')               AS invoice_no,
				        COALESCE(sse.cust_desc, '')                    AS cust_desc,
				        COALESCE(CAST(sse.net_value AS double precision), 0) AS net_value,
				        CASE da.status
				            WHEN 0  THEN 'PENDING'
				            WHEN 2  THEN 'DELIVERED'
				            WHEN 8  THEN 'REJECTED'
				            WHEN 9  THEN 'ASSIGNED'
				            WHEN 10 THEN 'CLOSED'
				            ELSE        'FAILED'
				        END                                            AS status,
				        COALESCE(ds.otp, false)                        AS otp,
				        ds.reason,
				        COALESCE(da.delivery_date, da.updated_at)      AS record_date,
				""" + com.api.distr.docs.sales.dto.PaymentDetailsUtil.COLS + """
				    FROM delivery_assignments da
				    LEFT JOIN delivery_status ds  ON ds.dire_id = da.dire_id
				    LEFT JOIN payment_details pd  ON pd.dire_id = da.dire_id
				    LEFT JOIN delivery_master dm  ON dm.delivery_id::text = da.delivery_boy_id
				    LEFT JOIN stage_sales_entery sse ON sse.dire_id = da.dire_id
				    WHERE da.status != 10
				      AND ((da.delivery_date BETWEEN ? AND ?)
				       OR  (da.status != 0 AND da.updated_at BETWEEN ? AND ?))
				    ORDER BY record_date DESC
				""";

		LocalDateTime from = fromDate.atStartOfDay();
		LocalDateTime to = toDate.atTime(23, 59, 59);

		List<DeliveryStatusDTO> result = jdbcTemplate.query(sql, new Object[] { from, to, from, to }, (rs, rowNum) -> {

			DeliveryStatusDTO dto = new DeliveryStatusDTO();

			dto.delivery_id     = rs.getString("delivery_id");
			dto.deliveryBoyName = rs.getString("delivery_name");
			dto.direId          = rs.getLong("dire_id");
			dto.picklist_no     = rs.getString("picklist_no");
			dto.invoiceNo       = rs.getString("invoice_no");
			dto.custDesc        = rs.getString("cust_desc");
			dto.netValue        = rs.getDouble("net_value");
			dto.status          = rs.getString("status");
			dto.otp             = rs.getBoolean("otp");
			dto.payment_amount  = rs.getDouble("pd_total");
			dto.reason          = rs.getString("reason");

			// Payment modes come from payment_details columns
			dto.paymentModes = com.api.distr.docs.sales.dto.PaymentDetailsUtil.fromResultSet(rs);

			// Format date
			LocalDateTime date = rs.getTimestamp("record_date") != null
					? rs.getTimestamp("record_date").toLocalDateTime()
					: null;
			dto.delivery_date = date != null ? date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "";

			return dto;
		});

		log.info("getByUpdateDate: returned {} records", result.size());
		return result;

		} catch (Exception e) {
			log.error("getByUpdateDate failed: fromDate={}, toDate={}, error={}", fromDate, toDate, e.getMessage(), e);
			throw e;
		}
	}
}
