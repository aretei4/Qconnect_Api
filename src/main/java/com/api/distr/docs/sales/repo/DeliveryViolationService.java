package com.api.distr.docs.sales.repo;

import com.api.distr.docs.sales.dto.DeliveryViolationDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

/**
 * Compares actual delivery location (delivery_status.lat/lon) against the
 * customer's registered address (customer_details.lat/lon).
 *
 * Other details (customer name, invoice no, net value) come from stage_sales_entery.
 * Agent name comes from delivery_master.
 *
 * Falls back to sample-violations.json when the DB returns no rows.
 */
@Service
public class DeliveryViolationService {

    private static final Logger log = LoggerFactory.getLogger(DeliveryViolationService.class);

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String SQL = """
            SELECT
                ds.dire_id                                                    AS dire_id,
                ds.delivery_id                                                AS agent_id,
                COALESCE(dm.delivery_name, 'Unknown')                        AS agent_name,
                COALESCE(sse.cust_desc,   '')                                AS cust_name,
                COALESCE(sse.customer_no, '')                                AS cust_no,
                COALESCE(sse.sales_order_no, '')                             AS invoice_no,
                TO_CHAR(ds.delivery_date, 'YYYY-MM-DD')                     AS delivery_date,
                ds.lat                                                        AS del_lat,
                ds.lon                                                        AS del_lng,
                COALESCE(cd.lat, 0)                                          AS cust_lat,
                COALESCE(cd.lon, 0)                                          AS cust_lng,
                COALESCE(CAST(sse.net_value AS DOUBLE PRECISION), 0.0)      AS net_value,
                COALESCE(ds.delivered, false)                                AS delivered
            FROM delivery_status ds
            JOIN  stage_sales_entery sse ON sse.dire_id   = ds.dire_id
            LEFT JOIN customer_details cd  ON cd.cust_no  = sse.customer_no
            LEFT JOIN delivery_master  dm  ON dm.delivery_id = ds.delivery_id
            WHERE ds.delivery_date BETWEEN ? AND ?
              AND ds.lat  IS NOT NULL AND ds.lat  <> 0
              AND ds.lon  IS NOT NULL AND ds.lon  <> 0
            ORDER BY ds.delivery_date DESC, dm.delivery_name
            """;

    public List<DeliveryViolationDTO> getViolations(LocalDate from, LocalDate to) {
        log.info("getViolations: from={} to={}", from, to);
        try {
            List<DeliveryViolationDTO> rows = jdbcTemplate.query(
                SQL,
                new Object[]{from, to},
                (rs, n) -> {
                    DeliveryViolationDTO d = new DeliveryViolationDTO();
                    d.direId    = rs.getLong("dire_id");
                    d.agentId   = rs.getLong("agent_id");
                    d.agentName = rs.getString("agent_name");
                    d.custName  = rs.getString("cust_name");
                    d.custNo    = rs.getString("cust_no");
                    d.invoiceNo = rs.getString("invoice_no");
                    d.date      = rs.getString("delivery_date");
                    d.delLat    = rs.getDouble("del_lat");
                    d.delLng    = rs.getDouble("del_lng");
                    d.custLat   = rs.getDouble("cust_lat");
                    d.custLng   = rs.getDouble("cust_lng");
                    d.netValue  = rs.getDouble("net_value");
                    d.delivered = rs.getBoolean("delivered");
                    return d;
                }
            );

            if (rows.isEmpty()) {
                log.warn("getViolations: no live data for {}-{}, returning sample data", from, to);
                return sampleData();
            }
            log.info("getViolations: returned {} rows", rows.size());
            return rows;

        } catch (Exception e) {
            log.error("getViolations failed: {}, returning sample data", e.getMessage(), e);
            return sampleData();
        }
    }

    private List<DeliveryViolationDTO> sampleData() {
        try {
            InputStream is = new ClassPathResource("sample-violations.json").getInputStream();
            DeliveryViolationDTO[] arr = objectMapper.readValue(is, DeliveryViolationDTO[].class);
            return Arrays.asList(arr);
        } catch (Exception e) {
            log.error("sampleData: failed to load sample-violations.json: {}", e.getMessage());
            return List.of();
        }
    }
}
