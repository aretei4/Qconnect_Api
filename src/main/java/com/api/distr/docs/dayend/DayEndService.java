package com.api.distr.docs.dayend;

import com.api.distr.docs.sales.dto.PaymentModeEntry;
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

        // ── 1. Aggregate totals ───────────────────────────────────────────────
        StringBuilder summarySql = new StringBuilder("""
            SELECT
                COUNT(*)                                      AS total,
                COUNT(*) FILTER (WHERE delivered = true)     AS delivered,
                COUNT(*) FILTER (WHERE delivered = false)    AS failed,
                COALESCE(SUM(payment_amount), 0)             AS total_amount
            FROM delivery_status
            WHERE delivery_date::date = ?
        """);

        List<Object> summaryParams = new ArrayList<>();
        summaryParams.add(date);

        if (deliveryId != null) {
            summarySql.append(" AND delivery_id = ?");
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
                    d.setTotalAmount(rs.getDouble("total_amount"));
                    return d;
                }
        );

        // ── 2. Payment breakdown — parse multi-mode strings in Java ───────────
        StringBuilder rawPaymentSql = new StringBuilder("""
            SELECT payment_mode, payment_amount
            FROM delivery_status
            WHERE delivery_date::date = ?
              AND delivered = true
              AND payment_mode IS NOT NULL
              AND payment_mode <> ''
        """);

        List<Object> paymentParams = new ArrayList<>();
        paymentParams.add(date);

        if (deliveryId != null) {
            rawPaymentSql.append(" AND delivery_id = ?");
            paymentParams.add(deliveryId);
        }

        List<Map<String, Object>> rawPaymentRows =
                jdbcTemplate.queryForList(rawPaymentSql.toString(), paymentParams.toArray());

        // Accumulate per-mode totals (handles both old "CASH" and new "CASH:1000.0,UPI:500.0")
        Map<String, Double> paymentMap = new LinkedHashMap<>();
        for (String m : List.of("Cash", "UPI", "Card", "Bank Transfer", "Credit")) {
            paymentMap.put(m, 0.0);
        }

        for (Map<String, Object> row : rawPaymentRows) {
            String pmStr     = (String) row.get("payment_mode");
            double rowTotal  = ((Number) row.get("payment_amount")).doubleValue();

            List<PaymentModeEntry> entries = parsePaymentModes(pmStr, rowTotal);
            for (PaymentModeEntry e : entries) {
                String key = normalizeMode(e.getMode());
                paymentMap.merge(key, e.getAmount(), Double::sum);
            }
        }

        dto.setAmountByPaymentMode(paymentMap);

        // ── 3. Per-picklist details (join delivery_status + stage_sales_entery) ─
        StringBuilder picklistSql = new StringBuilder("""
            SELECT
                ds.picklist_no,
                ds.delivered,
                ds.payment_amount,
                ds.payment_mode,
                ds.reason,
                COALESCE(sse.cust_desc, '')                         AS cust_desc,
                COALESCE(CAST(sse.net_value AS double precision), 0) AS net_value
            FROM delivery_status ds
            LEFT JOIN stage_sales_entery sse
                   ON TRIM(LOWER(sse.picklist_no)) = TRIM(LOWER(ds.picklist_no))
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
                    d.setPicklistNo(rs.getString("picklist_no"));
                    d.setCustDesc(rs.getString("cust_desc"));
                    d.setDelivered(rs.getBoolean("delivered"));
                    d.setNetValue(rs.getDouble("net_value"));
                    d.setPaymentAmount(rs.getDouble("payment_amount"));
                    d.setReason(rs.getString("reason"));

                    String pm = rs.getString("payment_mode");
                    d.setPaymentModes(parsePaymentModes(pm, rs.getDouble("payment_amount")));
                    return d;
                }
        );

        dto.setPicklists(picklists);

        // ── 4. Recalculate total from parsed paymentModes (more reliable than SUM(payment_amount)) ──
        double recalcTotal = picklists.stream()
                .filter(p -> p.getPaymentModes() != null)
                .flatMap(p -> p.getPaymentModes().stream())
                .mapToDouble(PaymentModeEntry::getAmount)
                .sum();

        if (recalcTotal > 0) {
            dto.setTotalAmount(recalcTotal);
        }

        return dto;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Parses both old single-mode ("CASH") and new multi-mode ("CASH:800.0,UPI:200.0") strings.
     * @param pmStr     raw payment_mode column value
     * @param rowTotal  payment_amount column value (fallback when old format has no amount)
     */
    private List<PaymentModeEntry> parsePaymentModes(String pmStr, double rowTotal) {
        List<PaymentModeEntry> list = new ArrayList<>();
        if (pmStr == null || pmStr.isBlank()) return list;

        for (String part : pmStr.split(",")) {
            part = part.trim();
            if (part.isEmpty()) continue;

            if (part.contains(":")) {
                // New format: "CASH:1000.0"
                String[] kv = part.split(":", 2);
                try {
                    String mode   = kv[0].trim();
                    double amount = Double.parseDouble(kv[1].trim());
                    list.add(new PaymentModeEntry(mode, amount));
                } catch (NumberFormatException ignored) { }
            } else {
                // Old format: just the mode name — use rowTotal as the amount
                list.add(new PaymentModeEntry(part, rowTotal));
            }
        }
        return list;
    }

    /**
     * Maps raw API/DB mode names to consistent display keys used in amountByPaymentMode.
     */
    private String normalizeMode(String mode) {
        if (mode == null) return "Other";
        switch (mode.toUpperCase().trim()) {
            case "CASH":                      return "Cash";
            case "UPI":                       return "UPI";
            case "CHEQUE": case "CARD":       return "Card";
            case "BANK_TRANSFER": case "ONLINE": return "Bank Transfer";
            case "CREDIT":                    return "Credit";
            default:                          return mode;
        }
    }
}
