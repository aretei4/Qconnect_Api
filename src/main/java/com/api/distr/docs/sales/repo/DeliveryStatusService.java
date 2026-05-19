package com.api.distr.docs.sales.repo;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import com.api.distr.docs.sales.dto.DeliveryStatusDTO;

@Service
public class DeliveryStatusService {

	@Autowired
	private JdbcTemplate jdbcTemplate;

	public List<DeliveryStatusDTO> getByUpdateDate(LocalDate fromDate, LocalDate toDate) {

		// delivery_assignments.status: 0=PENDING  1=FAILED  2=DELIVERED
		// delivery_assignments.delivery_date: set by UPDATE when boy submits (NULL while pending)
		// delivery_assignments.delivery_boy_id: VARCHAR → cast to text for JOIN with BIGINT delivery_master.delivery_id
		// delivery_status has no date column — never reference ds.updadate_date
		String sql = """
				    SELECT
				        da.delivery_boy_id                             AS delivery_id,
				        dm.delivery_name,
				        da.picklist_no,
				        CASE da.status
				            WHEN 0 THEN 'PENDING'
				            WHEN 2 THEN 'DELIVERED'
				            ELSE        'FAILED'
				        END                                            AS status,
				        COALESCE(ds.otp, false)                        AS otp,
				        COALESCE(ds.payment_amount, 0.0)               AS payment_amount,
				        ds.payment_mode,
				        ds.reason,
				        COALESCE(da.delivery_date, da.updated_at)      AS record_date
				    FROM delivery_assignments da
				    LEFT JOIN delivery_status ds ON ds.picklist_no = da.picklist_no
				    LEFT JOIN delivery_master dm ON dm.delivery_id::text = da.delivery_boy_id
				    WHERE COALESCE(da.delivery_date, da.updated_at) BETWEEN ? AND ?
				    ORDER BY record_date DESC
				""";

		LocalDateTime from = fromDate.atStartOfDay();
		LocalDateTime to = toDate.atTime(23, 59, 59);

		return jdbcTemplate.query(sql, new Object[] { from, to }, (rs, rowNum) -> {

			DeliveryStatusDTO dto = new DeliveryStatusDTO();

			dto.delivery_id = rs.getString("delivery_id");
			dto.deliveryBoyName = rs.getString("delivery_name");
			dto.picklist_no = rs.getString("picklist_no");
			dto.status = rs.getString("status");
			dto.otp = rs.getBoolean("otp");
			dto.payment_amount = rs.getDouble("payment_amount");
			dto.reason = rs.getString("reason");

			// Parse "CASH:1000.0,UPI:500.0" → List<PaymentModeEntry>
			String rawMode = rs.getString("payment_mode");
			dto.paymentModes = DeliveryStatusDTO.parsePaymentModes(rawMode, dto.payment_amount);

			// Format date
			LocalDateTime date = rs.getTimestamp("record_date") != null
					? rs.getTimestamp("record_date").toLocalDateTime()
					: null;
			dto.delivery_date = date != null ? date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "";

			return dto;
		});
	}
}
