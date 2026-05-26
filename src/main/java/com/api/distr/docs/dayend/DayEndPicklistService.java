package com.api.distr.docs.dayend;

import com.api.distr.docs.sales.dto.PicklistUpdateRequest;
import com.api.distr.docs.sales.dto.SalesEntryDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Day-end picklist management — view, validate and correct individual delivery
 * payment records from the Day-End management screen.
 */
@Service
public class DayEndPicklistService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * Returns picklists for a specific day-end record.
     *
     * delivery_status is the source of truth — it has one row per delivered/attempted
     * picklist for each delivery agent. We join dayend_approval → delivery_status
     * via delivery_id, then LEFT JOIN stage_sales_entery for invoice details and
     * delivery_assignments for the raw assignment status (0/1/2).
     */
    private static final String SELECT_PICKLISTS_BY_DAYEND = """
            SELECT
                TRIM(ds.picklist_no)                                     AS picklistNo,
                COALESCE(s.customer_no, '')                              AS customerNo,
                COALESCE(s.cust_desc,   '')                              AS custDesc,
                COALESCE(CAST(s.net_value AS DOUBLE PRECISION), 0.0)     AS netValue,
                COALESCE(da.status, 0)                                   AS assignStatus,
                COALESCE(ds.delivered, false)                            AS delivered,
                COALESCE(ds.payment_amount, 0.0)                         AS paymentAmount,
                ds.payment_mode                                          AS paymentMode,
                ds.reason                                                AS reason
            FROM dayend_approval dea
            INNER JOIN delivery_status ds
                ON  ds.delivery_id = dea.delivery_id
            LEFT JOIN stage_sales_entery s
                ON  TRIM(s.picklist_no) = TRIM(ds.picklist_no)
            LEFT JOIN delivery_assignments da
                ON  TRIM(da.picklist_no) = TRIM(ds.picklist_no)
            WHERE dea.id = ?
            ORDER BY TRIM(ds.picklist_no)
            """;

    /**
     * Returns all picklists belonging to a day-end record (by dayendId / dayend_approval.id)
     * with full payment and delivery-status detail. Used by the Day-End management screen.
     */
    public List<SalesEntryDto> getPicklistsByDayendId(Long dayendId) {
        return jdbcTemplate.query(
                SELECT_PICKLISTS_BY_DAYEND,
                new Object[]{dayendId},
                (rs, rowNum) -> {
                    SalesEntryDto dto = new SalesEntryDto();
                    dto.setPicklistNo(rs.getString("picklistNo"));
                    dto.setCustomerNo(rs.getString("customerNo"));
                    dto.setCustDesc(rs.getString("custDesc"));
                    dto.setNetValue("" + rs.getDouble("netValue"));
                    dto.setAssignStatus(rs.getInt("assignStatus"));
                    dto.setDelivered(rs.getBoolean("delivered"));
                    dto.setPaymentAmount(rs.getDouble("paymentAmount"));
                    dto.setPaymentMode(rs.getString("paymentMode"));
                    dto.setReason(rs.getString("reason"));
                    return dto;
                }
        );
    }

    /**
     * Updates payment and delivery status for a single picklist.
     * Uses check-then-update/insert to avoid needing a UNIQUE constraint on picklist_no.
     * delivery_id is looked up from delivery_assignments so the INSERT is valid.
     */
    public void updatePicklistPayment(String picklistNo, PicklistUpdateRequest req) {
        if (picklistNo == null || picklistNo.isBlank())
            throw new IllegalArgumentException("Picklist number is required");
        if (req.getPaymentAmount() < 0)
            throw new IllegalArgumentException("Payment amount cannot be negative");

        // 1. Check if a delivery_status row already exists for this picklist
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM delivery_status WHERE picklist_no = ?",
                Integer.class, picklistNo);

        if (count != null && count > 0) {
            // UPDATE existing row
            jdbcTemplate.update("""
                    UPDATE delivery_status
                    SET delivered      = ?,
                        payment_amount = ?,
                        payment_mode   = ?,
                        reason         = ?
                    WHERE picklist_no  = ?
                    """,
                    req.isDelivered(),
                    req.getPaymentAmount(),
                    req.getPaymentMode(),
                    req.getReason(),
                    picklistNo);
        } else {
            // Fetch delivery_id from delivery_assignments (needed for NOT NULL column)
            Long deliveryId = null;
            try {
                deliveryId = jdbcTemplate.queryForObject(
                        "SELECT delivery_boy_id::bigint FROM delivery_assignments WHERE picklist_no = ?",
                        Long.class, picklistNo);
            } catch (Exception ignored) { /* delivery_boy_id may not be numeric — leave null */ }

            jdbcTemplate.update("""
                    INSERT INTO delivery_status
                        (delivery_id, picklist_no, delivered, payment_amount, payment_mode, reason)
                    VALUES (?, ?, ?, ?, ?, ?)
                    """,
                    deliveryId,
                    picklistNo,
                    req.isDelivered(),
                    req.getPaymentAmount(),
                    req.getPaymentMode(),
                    req.getReason());
        }

        // 2. Sync delivery_assignments.status  (2=DELIVERED, 1=FAILED)
        int status = req.isDelivered() ? 2 : 1;
        jdbcTemplate.update(
                "UPDATE delivery_assignments SET status = ? WHERE picklist_no = ?",
                status, picklistNo);
    }

    /**
     * Deletes a picklist from delivery_assignments (and cascades to delivery_status
     * if ON DELETE CASCADE is set, otherwise cleans up separately).
     */
    public int deletePicklist(String picklistNo) {
        // Clean up delivery_status first (no cascade assumption)
        jdbcTemplate.update(
                "DELETE FROM delivery_status WHERE picklist_no = ?", picklistNo);
        return jdbcTemplate.update(
                "DELETE FROM delivery_assignments WHERE picklist_no = ?", picklistNo);
    }
}
