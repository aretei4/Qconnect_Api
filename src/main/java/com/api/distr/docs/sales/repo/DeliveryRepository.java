package com.api.distr.docs.sales.repo;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import com.api.distr.docs.sales.dto.DeliveryAgent;
import com.api.distr.docs.sales.dto.DeliveryLoginResponse;
import com.api.distr.docs.sales.dto.DeliveryRequest;
import com.api.distr.docs.sales.dto.DeliveryStatus;
import com.api.distr.docs.sales.dto.SalesEntryDto;

@Repository
public class DeliveryRepository {

	private final JdbcTemplate jdbcTemplate;

	public DeliveryRepository(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	public DeliveryLoginResponse findByMobile(String mobile) {      

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
    }
	
	public void upsertByPicklistNo(DeliveryStatus d) {
		String checkSql = "SELECT COUNT(*) FROM delivery_status WHERE picklist_no = ?";
		Integer count = jdbcTemplate.queryForObject(checkSql, Integer.class, d.getPicklistNo());
		String updateAssign = """
				UPDATE delivery_assignments
				SET status=?,
				delivery_date = CURRENT_TIMESTAMP
				WHERE picklist_no=?""";
		int status = 1;
		if (d.isDelivered()) {
			status = 2;
		}
		jdbcTemplate.update(updateAssign, status,d.getPicklistNo());

		// payment_mode  → "CASH:1000.0,UPI:500.0"
		// payment_amount → sum of all mode amounts
		String paymentModeValue  = d.getPaymentModeDbValue();
		double paymentAmountTotal = d.getTotalPaymentAmount();

		if (count != null && count > 0) {
			// Update existing record
			String updateSql = """
					UPDATE delivery_status
					SET delivered=?, otp=?, payment_amount=?, payment_mode=?, reason=?
					WHERE picklist_no=?""";
			jdbcTemplate.update(updateSql, d.isDelivered(), d.isOtp(), paymentAmountTotal, paymentModeValue,
					d.getReason(), d.getPicklistNo());
		} else {
			// Insert new record
			String insertSql = """
					INSERT INTO delivery_status (delivery_id, delivered, otp, payment_amount, payment_mode, picklist_no, reason, lat, lon)
					VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)""";
			jdbcTemplate.update(insertSql, d.getDelivery_id(), d.isDelivered(), d.isOtp(), paymentAmountTotal,
					paymentModeValue, d.getPicklistNo(), d.getReason(), d.getLat(), d.getLon());
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
			jdbcTemplate.update(sql, picklistNo, request.getDeliveryBoyId(), request.getCar().getCarNo(),
					request.getCar().getDriverName(), request.getCar().getMobile(), LocalDateTime.now());
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

	public int deleteByPicklistNo(String picklistNo) {
		String sql = QueryConstants.DELETE_DELIVERY_ASSIGN;
		return jdbcTemplate.update(sql, picklistNo);
	}

	public List<SalesEntryDto> getAllSales(String deliveryId) {

	    String sql = QueryConstants.SELECT_DELIVERY_ASSIGN+deliveryId+"')"; //+ " ORDER BY update_date DESC";

	    System.out.println("**************** " + sql);

	    return jdbcTemplate.query(sql, (rs, rowNum) -> {

	        SalesEntryDto dto = new SalesEntryDto();

	        // 👇 Values are SET here
	        dto.setPicklistNo(rs.getString("picklistNo"));
	       // dto.setSalesOrderNo(rs.getString("sales_order_no"));
	        dto.setCustomerNo(rs.getString("customerNo"));
        dto.setCustDesc(rs.getString("custDesc"));
	        dto.setNetValue(""+rs.getDouble("netValue"));
	        // dto.setUpdateDate(rs.getTimestamp("update_date"));

	        return dto;   // 🔴 THIS WAS MISSING
	    });
	}

}
