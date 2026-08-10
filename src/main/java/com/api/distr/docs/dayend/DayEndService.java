package com.api.distr.docs.dayend;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;

@Service
public class DayEndService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    public DayEndSummary getDayEndSummary(LocalDate date, Long deliveryId) {

        // ── 1. Delivery counts (delivery_status only — money comes from §2) ────
        StringBuilder summarySql = new StringBuilder("""
            SELECT
                COUNT(*)                                     AS total,
                COUNT(*) FILTER (WHERE ds.delivered = true)  AS delivered,
                COUNT(*) FILTER (WHERE ds.delivered = false) AS failed
            FROM delivery_status ds
            WHERE ds.delivery_date::date = ?
        """);

        List<Object> summaryParams = new ArrayList<>();
        summaryParams.add(date);

        if (deliveryId != null) {
            summarySql.append(" AND ds.delivery_id = ?");
            summaryParams.add(deliveryId);
        }

        DayEndSummary dto = jdbcTemplate.queryForObject(
                summarySql.toString(),
                summaryParams.toArray(),
                (rs, rowNum) -> {
                    DayEndSummary d = new DayEndSummary();
                    d.setTotal(rs.getLong("total"));
                    d.setDelivered(rs.getLong("delivered"));
                    d.setFailed(rs.getLong("failed"));
                    return d;
                }
        );

        // ── 2. All money figures — every value comes from payment_details ──────
        // NOTE: deliberately NOT filtered on ds.delivered. The old filter was
        // `ds.delivered = true`, which drops rows where delivered is false OR NULL;
        // their credit_amount vanished from the breakdown while still counting
        // towards the total, so "Credit amount" rendered as ₹0.00 and cash
        // collected was overstated by exactly that amount.
        StringBuilder breakdownSql = new StringBuilder("""
            SELECT
                COALESCE(SUM(pd.cash_amount), 0)   AS cash,
                COALESCE(SUM(pd.upi_amount), 0)    AS upi,
                COALESCE(SUM(pd.cheque_amount), 0) AS cheque,
                COALESCE(SUM(pd.neft_amount), 0)   AS neft,
                COALESCE(SUM(pd.credit_amount), 0) AS credit,
                COALESCE(SUM(pd.return_amount), 0) AS return_amount,
                COALESCE(SUM(pd.total_amount), 0)  AS total_amount
            FROM delivery_status ds
            JOIN payment_details pd ON pd.dire_id = ds.dire_id
            WHERE ds.delivery_date::date = ?
        """);

        List<Object> paymentParams = new ArrayList<>();
        paymentParams.add(date);

        if (deliveryId != null) {
            breakdownSql.append(" AND ds.delivery_id = ?");
            paymentParams.add(deliveryId);
        }

        Map<String, Double> paymentMap = new LinkedHashMap<>();
        for (String m : List.of("Cash", "UPI", "Card", "Bank Transfer", "Credit")) {
            paymentMap.put(m, 0.0);
        }
        jdbcTemplate.query(breakdownSql.toString(), paymentParams.toArray(), rs -> {
            double credit = rs.getDouble("credit");
            double total  = rs.getDouble("total_amount");

            paymentMap.put("Cash",          rs.getDouble("cash"));
            paymentMap.put("UPI",           rs.getDouble("upi"));
            // Cheque folds into the existing "Card" display bucket
            paymentMap.put("Card",          rs.getDouble("cheque"));
            paymentMap.put("Bank Transfer", rs.getDouble("neft"));
            paymentMap.put("Credit",        credit);

            // Explicit figures so the client never has to derive them by arithmetic
            dto.setTotalAmount(total);                       // includes credit
            dto.setCreditAmount(credit);
            dto.setReturnAmount(rs.getDouble("return_amount"));
            dto.setCollectedAmount(total - credit);          // actual cash-equivalent collected
        });

        dto.setAmountByPaymentMode(paymentMap);

        // ── 3. Per-picklist details (join delivery_status + stage_sales_entery) ─
        StringBuilder picklistSql = new StringBuilder("""
            SELECT
                ds.picklist_no,
                ds.delivered,
                ds.reason,
                COALESCE(ds.dire_id, 0)                             AS dire_id,
                COALESCE(sse.sales_order_no, '')                    AS invoice_no,
                COALESCE(sse.cust_desc, '')                         AS cust_desc,
                COALESCE(CAST(sse.net_value AS double precision), 0) AS net_value,
        """ + com.api.distr.docs.sales.dto.PaymentDetailsUtil.COLS + """
            FROM delivery_status ds
            LEFT JOIN payment_details pd ON pd.dire_id = ds.dire_id
            LEFT JOIN stage_sales_entery sse ON sse.dire_id = ds.dire_id
            WHERE ds.delivery_date::date = ?
        """);

        List<Object> picklistParams = new ArrayList<>();
        picklistParams.add(date);

        if (deliveryId != null) {
            picklistSql.append(" AND ds.delivery_id = ?");
            picklistParams.add(deliveryId);
        }

        picklistSql.append(" ORDER BY ds.picklist_no");

        List<DayEndPicklistDetail> picklists = jdbcTemplate.query(
                picklistSql.toString(),
                picklistParams.toArray(),
                (rs, rowNum) -> {
                    DayEndPicklistDetail d = new DayEndPicklistDetail();
                    d.setDireId(rs.getLong("dire_id"));
                    d.setInvoiceNo(rs.getString("invoice_no"));
                    d.setPicklistNo(rs.getString("picklist_no"));
                    d.setCustDesc(rs.getString("cust_desc"));
                    d.setDelivered(rs.getBoolean("delivered"));
                    d.setNetValue(rs.getDouble("net_value"));
                    d.setPaymentAmount(rs.getDouble("pd_total"));
                    d.setReason(rs.getString("reason"));
                    d.setPaymentModes(com.api.distr.docs.sales.dto.PaymentDetailsUtil.fromResultSet(rs));
                    return d;
                }
        );

        dto.setPicklists(picklists);

        // NOTE: totalAmount is set in §2 straight from SUM(payment_details.total_amount).
        // The old post-hoc recalculation from the parsed per-picklist modes was removed —
        // it re-derived a figure payment_details already stores and could disagree with
        // the credit/return values above.

        return dto;
    }
}
