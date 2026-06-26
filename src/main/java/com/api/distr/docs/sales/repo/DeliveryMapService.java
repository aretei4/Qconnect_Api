package com.api.distr.docs.sales.repo;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import com.api.distr.docs.sales.dto.DeliveryMapDTO;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Returns delivery map points with coordinates AND net_value.
 *
 * Query strategy:
 *   delivery_assignments  — gives picklist_no, delivery_boy_id, status, lat, lon
 *   delivery_master       — resolves delivery_boy_id → delivery_name
 *   stage_sales_entery    — gives net_value (and customer info) per picklist_no
 *
 * Falls back to static demo data when the DB returns no rows for the requested
 * date range (e.g. on a development machine with no live assignments).
 */
@Service
public class DeliveryMapService {

    private static final Logger log = LoggerFactory.getLogger(DeliveryMapService.class);

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    // ── Live DB query ──────────────────────────────────────────────────────────
    //
    // Columns returned:
    //   picklist_no, deliveryBoyName, status (0→PENDING / 1→FAILED / 2→DELIVERED),
    //   lat, lon, delivery_date (from da), address, sequence, net_value
    //
    // net_value comes from stage_sales_entery; NULL → 0.0 via COALESCE.
    // lat/lon come from delivery_assignments — ensure those columns exist in the
    // table before switching to this query in production.
    private static final String SQL = """
            SELECT
                da.dire_id                                                  AS dire_id,
                COALESCE(dm.delivery_name, 'Unknown')                       AS delivery_boy_name,
                CASE da.status
                    WHEN 2 THEN 'DELIVERED'
                    WHEN 1 THEN 'FAILED'
                    ELSE        'PENDING'
                END                                                         AS status,
                COALESCE(da.lat,  0)                                        AS lat,
                COALESCE(da.lon,  0)                                        AS lon,
                TO_CHAR(da.delivery_date, 'DD/MM/YYYY')                    AS delivery_date,
                COALESCE(da.address, '')                                    AS address,
                COALESCE(da.sequence, 0)                                    AS sequence,
                COALESCE(CAST(sse.net_value AS DOUBLE PRECISION), 0.0)     AS net_value
            FROM delivery_assignments da
            LEFT JOIN delivery_master dm
                   ON da.delivery_boy_id::text = dm.delivery_id::text
            LEFT JOIN stage_sales_entery sse
                   ON sse.dire_id = da.dire_id
            WHERE da.delivery_date BETWEEN ? AND ?
              AND COALESCE(da.lat, 0) <> 0
              AND COALESCE(da.lon, 0) <> 0
            ORDER BY dm.delivery_name, da.sequence
            """;

    public List<DeliveryMapDTO> getMapPoints(LocalDate fromDate, LocalDate toDate) {
        log.info("getMapPoints: fromDate={}, toDate={}", fromDate, toDate);
        try {
            List<DeliveryMapDTO> rows = jdbcTemplate.query(
                    SQL,
                    new Object[]{fromDate, toDate},
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

            if (rows.isEmpty()) {
                log.warn("getMapPoints: no live rows found for range {}-{}, returning demo data", fromDate, toDate);
                return staticDemoPoints();
            }
            log.info("getMapPoints: returned {} map points", rows.size());
            return rows;

        } catch (Exception e) {
            log.error("getMapPoints failed: fromDate={}, toDate={}, error={} — returning demo data",
                    fromDate, toDate, e.getMessage(), e);
            return staticDemoPoints();
        }
    }

    // ── Static demo data (used until delivery_assignments has lat/lon columns) ──

    private static final String STATIC_JSON = """
            [
              { "picklist_no": "E587P22657", "deliveryBoyName": "Ravi Kumar",  "status": "PENDING", "lat": 20.2961, "lon": 85.8245, "delivery_date": "19/05/2026", "address": "Rajmahal Square, Bhubaneswar",    "sequence": 1, "net_value": 1250.00 },
              { "picklist_no": "E587P22659", "deliveryBoyName": "Ravi Kumar",  "status": "PENDING", "lat": 20.2721, "lon": 85.8411, "delivery_date": "19/05/2026", "address": "Saheed Nagar, Bhubaneswar",       "sequence": 2, "net_value": 3400.00 },
              { "picklist_no": "E587P22665", "deliveryBoyName": "Ravi Kumar",  "status": "PENDING", "lat": 20.2560, "lon": 85.8480, "delivery_date": "19/05/2026", "address": "Satya Nagar, Bhubaneswar",        "sequence": 3, "net_value":  875.50 },
              { "picklist_no": "E587P22669", "deliveryBoyName": "Ravi Kumar",  "status": "PENDING", "lat": 20.2490, "lon": 85.8170, "delivery_date": "19/05/2026", "address": "Jagamara, Bhubaneswar",           "sequence": 4, "net_value": 2100.00 },
              { "picklist_no": "E587P22673", "deliveryBoyName": "Ravi Kumar",  "status": "PENDING", "lat": 20.2670, "lon": 85.8730, "delivery_date": "19/05/2026", "address": "Nayapalli, Bhubaneswar",          "sequence": 5, "net_value": 4750.00 },

              { "picklist_no": "E587P22658", "deliveryBoyName": "Suresh Jena", "status": "PENDING", "lat": 20.3085, "lon": 85.8382, "delivery_date": "19/05/2026", "address": "Bapuji Nagar, Bhubaneswar",       "sequence": 1, "net_value":  620.00 },
              { "picklist_no": "E587P22661", "deliveryBoyName": "Suresh Jena", "status": "PENDING", "lat": 20.2830, "lon": 85.8560, "delivery_date": "19/05/2026", "address": "IRC Village, Bhubaneswar",        "sequence": 2, "net_value": 1890.00 },
              { "picklist_no": "E587P22666", "deliveryBoyName": "Suresh Jena", "status": "PENDING", "lat": 20.3010, "lon": 85.7980, "delivery_date": "19/05/2026", "address": "Patia, Bhubaneswar",              "sequence": 3, "net_value": 3200.00 },
              { "picklist_no": "E587P22670", "deliveryBoyName": "Suresh Jena", "status": "PENDING", "lat": 20.3440, "lon": 85.8360, "delivery_date": "19/05/2026", "address": "Chandrasekharpur, Bhubaneswar",   "sequence": 4, "net_value":  450.00 },
              { "picklist_no": "E587P22674", "deliveryBoyName": "Suresh Jena", "status": "PENDING", "lat": 20.3520, "lon": 85.8450, "delivery_date": "19/05/2026", "address": "Infocity, Bhubaneswar",           "sequence": 5, "net_value": 5600.00 },

              { "picklist_no": "E587P22660", "deliveryBoyName": "Manoj Nayak", "status": "PENDING", "lat": 20.3210, "lon": 85.8156, "delivery_date": "19/05/2026", "address": "Damana Square, Bhubaneswar",      "sequence": 1, "net_value": 2350.00 },
              { "picklist_no": "E587P22663", "deliveryBoyName": "Manoj Nayak", "status": "PENDING", "lat": 20.2640, "lon": 85.8320, "delivery_date": "19/05/2026", "address": "Kalpana Square, Bhubaneswar",     "sequence": 2, "net_value":  980.00 },
              { "picklist_no": "E587P22667", "deliveryBoyName": "Manoj Nayak", "status": "PENDING", "lat": 20.2780, "lon": 85.8620, "delivery_date": "19/05/2026", "address": "Bomikhal, Bhubaneswar",           "sequence": 3, "net_value": 1650.00 },
              { "picklist_no": "E587P22671", "deliveryBoyName": "Manoj Nayak", "status": "PENDING", "lat": 20.2900, "lon": 85.8720, "delivery_date": "19/05/2026", "address": "Rasulgarh, Bhubaneswar",          "sequence": 4, "net_value": 4100.00 },
              { "picklist_no": "E587P22675", "deliveryBoyName": "Manoj Nayak", "status": "PENDING", "lat": 20.2410, "lon": 85.8390, "delivery_date": "19/05/2026", "address": "Lingaraj Nagar, Bhubaneswar",     "sequence": 5, "net_value":  730.00 },

              { "picklist_no": "E587P22662", "deliveryBoyName": "Arjun Sahoo", "status": "PENDING", "lat": 20.3150, "lon": 85.8470, "delivery_date": "19/05/2026", "address": "Niladri Vihar, Bhubaneswar",      "sequence": 1, "net_value": 3750.00 },
              { "picklist_no": "E587P22664", "deliveryBoyName": "Arjun Sahoo", "status": "PENDING", "lat": 20.3380, "lon": 85.8240, "delivery_date": "19/05/2026", "address": "Kalinga Nagar, Bhubaneswar",      "sequence": 2, "net_value": 1120.00 },
              { "picklist_no": "E587P22668", "deliveryBoyName": "Arjun Sahoo", "status": "PENDING", "lat": 20.3260, "lon": 85.8550, "delivery_date": "19/05/2026", "address": "Airport Road, Bhubaneswar",       "sequence": 3, "net_value": 2800.00 },
              { "picklist_no": "E587P22672", "deliveryBoyName": "Arjun Sahoo", "status": "PENDING", "lat": 20.3100, "lon": 85.8050, "delivery_date": "19/05/2026", "address": "Nandankanan Road, Bhubaneswar",   "sequence": 4, "net_value":  560.00 },
              { "picklist_no": "E587P22676", "deliveryBoyName": "Arjun Sahoo", "status": "PENDING", "lat": 20.3300, "lon": 85.8680, "delivery_date": "19/05/2026", "address": "Baramunda, Bhubaneswar",          "sequence": 5, "net_value": 6200.00 }
            ]
            """;

    private List<DeliveryMapDTO> staticDemoPoints() {
        try {
            DeliveryMapDTO[] arr = objectMapper.readValue(STATIC_JSON, DeliveryMapDTO[].class);
            return Arrays.asList(arr);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse static demo data: " + e.getMessage(), e);
        }
    }
}
