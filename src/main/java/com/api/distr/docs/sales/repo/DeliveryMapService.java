package com.api.distr.docs.sales.repo;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import com.api.distr.docs.sales.dto.DeliveryMapDTO;

/**
 * Returns ASSIGNED (status 9) delivery map points for one agent.
 * Coordinates come from customer_details; net_value from stage_sales_entery.
 * Returns an empty list when the agent has no assigned stops — no demo data.
 */
@Service
public class DeliveryMapService {

    private static final Logger log = LoggerFactory.getLogger(DeliveryMapService.class);

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // ── Live DB query ──────────────────────────────────────────────────────────
    //
    // ASSIGNED (status 9) deliveries for one agent.
    // lat/lon come from customer_details (customer master, keyed by cust_no);
    // net_value and customer info from stage_sales_entery.
    private static final String SQL = """
            SELECT
                da.dire_id                                                  AS dire_id,
                COALESCE(dm.delivery_name, 'Unknown')                       AS delivery_boy_name,
                CASE da.status
                    WHEN 9 THEN 'ASSIGNED'
                    ELSE        'PENDING'
                END                                                         AS status,
                COALESCE(cd.lat,  0)                                        AS lat,
                COALESCE(cd.lon,  0)                                        AS lon,
                TO_CHAR(COALESCE(da.delivery_date, da.updated_at), 'DD/MM/YYYY') AS delivery_date,
                COALESCE(sse.cust_desc, '')                                 AS address,
                COALESCE(da.sequence, 0)                                    AS sequence,
                COALESCE(CAST(sse.net_value AS DOUBLE PRECISION), 0.0)     AS net_value
            FROM delivery_assignments da
            LEFT JOIN delivery_master dm
                   ON da.delivery_boy_id::text = dm.delivery_id::text
            LEFT JOIN stage_sales_entery sse
                   ON sse.dire_id = da.dire_id
            LEFT JOIN customer_details cd
                   ON cd.cust_no = sse.customer_no
            WHERE da.status IN (0, 9)
              AND da.delivery_boy_id = ?
              AND COALESCE(cd.lat, 0) <> 0
              AND COALESCE(cd.lon, 0) <> 0
            ORDER BY da.sequence, da.dire_id
            """;

    public List<DeliveryMapDTO> getMapPoints(String deliveryBoyId) {
        log.info("getMapPoints: deliveryBoyId={}", deliveryBoyId);
        try {
            List<DeliveryMapDTO> rows = jdbcTemplate.query(
                    SQL,
                    new Object[]{deliveryBoyId},
                    (rs, rowNum) -> {
                        DeliveryMapDTO d = new DeliveryMapDTO();
                        d.picklist_no     = String.valueOf(rs.getLong("dire_id"));
                        d.deliveryBoyName = rs.getString("delivery_boy_name");
                        d.status          = rs.getString("status");
                        d.lat             = rs.getDouble("lat");
                        d.lon             = rs.getDouble("lon");
                        d.delivery_date   = rs.getString("delivery_date");
                        d.address         = rs.getString("address");
                        d.sequence        = rs.getInt("sequence");
                        d.net_value       = rs.getDouble("net_value");
                        return d;
                    }
            );

            log.info("getMapPoints: returned {} map points for deliveryBoyId={}", rows.size(), deliveryBoyId);
            return rows;

        } catch (Exception e) {
            log.error("getMapPoints failed: deliveryBoyId={}, error={}", deliveryBoyId, e.getMessage(), e);
            throw e;
        }
    }

}
