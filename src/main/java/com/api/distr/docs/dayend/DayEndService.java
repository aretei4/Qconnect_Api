package com.api.distr.docs.dayend;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class DayEndService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    public DayEndSummary getDayEndSummary(LocalDate date, Long deliveryId) {

        StringBuilder summarySql = new StringBuilder("""
            SELECT 
                COUNT(*) AS total,
                COUNT(*) FILTER (WHERE delivered = true) AS delivered,
                COUNT(*) FILTER (WHERE delivered = false) AS failed,
                COALESCE(SUM(payment_amount),0) AS total_amount
            FROM delivery_status
            WHERE delivery_date::date = ?
        """);

        List<Object> params = new ArrayList<>();
        params.add(date);

        if (deliveryId != null) {
            summarySql.append(" AND delivery_id = ?");
            params.add(deliveryId);
        }

        DayEndSummary dto = jdbcTemplate.queryForObject(
                summarySql.toString(),
                params.toArray(),
                (rs, rowNum) -> {
                    DayEndSummary d = new DayEndSummary();
                    d.setTotal(rs.getLong("total"));
                    d.setDelivered(rs.getLong("delivered"));
                    d.setFailed(rs.getLong("failed"));
                    d.setTotalAmount(rs.getDouble("total_amount"));
                    return d;
                }
        );

        // Payment mode query
        StringBuilder paymentSql = new StringBuilder("""
        	    SELECT payment_mode,
        	           COALESCE(SUM(payment_amount),0) AS amount
        	    FROM delivery_status
        	    WHERE delivery_date::date = ?
        	      AND delivered = true
        	      AND payment_mode IS NOT NULL
        	      AND payment_mode <> ''
        	""");

        List<Object> paymentParams = new ArrayList<>();
        paymentParams.add(date);

        if (deliveryId != null) {
            paymentSql.append(" AND delivery_id = ?");
            paymentParams.add(deliveryId);
        }

        paymentSql.append(" GROUP BY payment_mode");

        List<Map<String, Object>> rows =
                jdbcTemplate.queryForList(paymentSql.toString(), paymentParams.toArray());

        List<String> allModes = List.of("Cash", "UPI", "CARD", "ONLINE", "Credit");

        Map<String, Double> paymentMap = new LinkedHashMap<>();

        // Initialize all modes with 0
        for (String mode : allModes) {
            paymentMap.put(mode, 0.0);
        }

        // Fill actual DB values
        for (Map<String, Object> row : rows) {

            String mode = (String) row.get("payment_mode");

            // 🚫 Skip blank or null
            if (mode == null || mode.trim().isEmpty()) {
                continue;
            }

            Double amount = ((Number) row.get("amount")).doubleValue();

            paymentMap.put(mode, amount);
        }

        dto.setAmountByPaymentMode(paymentMap);

        return dto;
    }
}