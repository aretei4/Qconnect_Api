package com.api.distr.docs.dayend;

import com.api.distr.docs.sales.dto.PicklistUpdateRequest;
import com.api.distr.docs.sales.dto.SalesEntryDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger log = LoggerFactory.getLogger(DayEndPicklistService.class);

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
                ds.dire_id                                               AS direId,
                COALESCE(s.sales_order_no, '')                           AS invoiceNo,
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
                ON  ds.delivery_id      = dea.delivery_id
                AND ds.delivery_date::date = dea.delivery_date
            LEFT JOIN stage_sales_entery s
                ON  s.dire_id = ds.dire_id
            LEFT JOIN delivery_assignments da
                ON  da.dire_id = ds.dire_id
            WHERE dea.id = ?
              AND COALESCE(da.status, 0) != 10
            ORDER BY ds.dire_id
            """;

    /**
     * Returns all picklists belonging to a day-end record (by dayendId / dayend_approval.id)
     * with full payment and delivery-status detail. Used by the Day-End management screen.
     */
    public List<SalesEntryDto> getPicklistsByDayendId(Long dayendId) {
        log.info("getPicklistsByDayendId: dayendId={}", dayendId);
        try {
            List<SalesEntryDto> result = jdbcTemplate.query(
                    SELECT_PICKLISTS_BY_DAYEND,
                    new Object[]{dayendId},
                    (rs, rowNum) -> {
                        SalesEntryDto dto = new SalesEntryDto();
                        dto.setDireId(rs.getLong("direId"));
                        dto.setInvoiceNo(rs.getString("invoiceNo"));
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
            log.info("getPicklistsByDayendId: returned {} picklists for dayendId={}", result.size(), dayendId);
            return result;
        } catch (Exception e) {
            log.error("getPicklistsByDayendId failed: dayendId={}, error={}", dayendId, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Updates payment and delivery status for a single picklist.
     * Uses check-then-update/insert to avoid needing a UNIQUE constraint on picklist_no.
     * delivery_id is looked up from delivery_assignments so the INSERT is valid.
     */
    public void updatePicklistPayment(Long direId, PicklistUpdateRequest req) {
        if (direId == null || direId <= 0)
            throw new IllegalArgumentException("direId is required");
        if (req.getPaymentAmount() < 0)
            throw new IllegalArgumentException("Payment amount cannot be negative");

        log.info("updatePicklistPayment: direId={}, delivered={}, amount={}",
                direId, req.isDelivered(), req.getPaymentAmount());
        try {
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM delivery_status WHERE dire_id = ?",
                    Integer.class, direId);

            if (count != null && count > 0) {
                jdbcTemplate.update("""
                        UPDATE delivery_status
                        SET delivered      = ?,
                            payment_amount = ?,
                            payment_mode   = ?,
                            reason         = ?
                        WHERE dire_id      = ?
                        """,
                        req.isDelivered(), req.getPaymentAmount(),
                        req.getPaymentMode(), req.getReason(), direId);
            } else {
                Long deliveryId = null;
                try {
                    deliveryId = jdbcTemplate.queryForObject(
                            "SELECT delivery_boy_id::bigint FROM delivery_assignments WHERE dire_id = ?",
                            Long.class, direId);
                } catch (Exception ignored) {
                    log.warn("updatePicklistPayment: could not resolve delivery_id for direId={}", direId);
                }
                jdbcTemplate.update("""
                        INSERT INTO delivery_status
                            (dire_id, delivery_id, delivered, payment_amount, payment_mode, reason)
                        VALUES (?, ?, ?, ?, ?, ?)
                        """,
                        direId, deliveryId,
                        req.isDelivered(), req.getPaymentAmount(),
                        req.getPaymentMode(), req.getReason());
            }

            int status = req.isDelivered() ? 2 : 1;
            jdbcTemplate.update(
                    "UPDATE delivery_assignments SET status = ? WHERE dire_id = ?",
                    status, direId);

            log.info("updatePicklistPayment: completed for direId={}", direId);
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            log.error("updatePicklistPayment failed: direId={}, error={}", direId, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Deletes a picklist from delivery_assignments (and cascades to delivery_status
     * if ON DELETE CASCADE is set, otherwise cleans up separately).
     */
    public int deletePicklist(Long direId) {
        log.info("deletePicklist: direId={}", direId);
        try {
            jdbcTemplate.update("DELETE FROM delivery_status WHERE dire_id = ?", direId);
            int rows = jdbcTemplate.update("DELETE FROM delivery_assignments WHERE dire_id = ?", direId);
            log.info("deletePicklist: deleted {} assignment row(s) for direId={}", rows, direId);
            return rows;
        } catch (Exception e) {
            log.error("deletePicklist failed: direId={}, error={}", direId, e.getMessage(), e);
            throw e;
        }
    }
}
