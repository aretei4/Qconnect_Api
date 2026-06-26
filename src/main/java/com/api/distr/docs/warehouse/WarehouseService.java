package com.api.distr.docs.warehouse;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;


@Service
public class WarehouseService {

    private static final Logger log = LoggerFactory.getLogger(WarehouseService.class);

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // ── READ ──────────────────────────────────────────────────────────────────

    /** Returns all active warehouses that have valid coordinates. */
    public List<WarehouseDTO> getActiveWarehouses() {
        log.info("getActiveWarehouses: fetching active warehouses");
        try {
            List<WarehouseDTO> result = jdbcTemplate.query("""
                    SELECT id, name, address, lat, lon, active
                    FROM warehouse_master
                    WHERE active = true
                      AND lat IS NOT NULL
                      AND lon IS NOT NULL
                    ORDER BY id
                    """, this::mapRow);
            log.info("getActiveWarehouses: returned {} warehouses", result.size());
            return result;
        } catch (Exception e) {
            log.error("getActiveWarehouses failed, using static fallback: error={}", e.getMessage(), e);
            return staticFallback();
        }
    }

    /** Returns all warehouses (active + inactive) — for management screen. */
    public List<WarehouseDTO> getAllWarehouses() {
        log.info("getAllWarehouses: fetching all warehouses");
        try {
            List<WarehouseDTO> result = jdbcTemplate.query("""
                    SELECT id, name, address, lat, lon, active
                    FROM warehouse_master
                    ORDER BY id
                    """, this::mapRow);
            log.info("getAllWarehouses: returned {} warehouses", result.size());
            return result;
        } catch (Exception e) {
            log.error("getAllWarehouses failed, using static fallback: error={}", e.getMessage(), e);
            return staticFallback();
        }
    }

    // ── CREATE ────────────────────────────────────────────────────────────────

    public WarehouseDTO createWarehouse(WarehouseDTO dto) {
        validate(dto);
        log.info("createWarehouse: name={}, lat={}, lon={}", dto.name, dto.lat, dto.lon);
        try {
            KeyHolder keyHolder = new GeneratedKeyHolder();
            jdbcTemplate.update(con -> {
                PreparedStatement ps = con.prepareStatement("""
                        INSERT INTO warehouse_master (name, address, lat, lon, active)
                        VALUES (?, ?, ?, ?, true)
                        """, Statement.RETURN_GENERATED_KEYS);
                ps.setString(1, dto.name.trim());
                ps.setString(2, dto.address != null ? dto.address.trim() : "");
                ps.setDouble(3, dto.lat);
                ps.setDouble(4, dto.lon);
                return ps;
            }, keyHolder);

            Number key = keyHolder.getKey();
            dto.id     = key != null ? key.longValue() : null;
            dto.active = true;
            log.info("createWarehouse: created warehouse id={}", dto.id);
            return dto;
        } catch (Exception e) {
            log.error("createWarehouse failed: name={}, error={}", dto.name, e.getMessage(), e);
            throw e;
        }
    }

    // ── UPDATE ────────────────────────────────────────────────────────────────

    public void updateWarehouse(Long id, WarehouseDTO dto) {
        validate(dto);
        log.info("updateWarehouse: id={}, name={}", id, dto.name);
        try {
            int rows = jdbcTemplate.update("""
                    UPDATE warehouse_master
                    SET name    = ?,
                        address = ?,
                        lat     = ?,
                        lon     = ?,
                        active  = ?
                    WHERE id = ?
                    """,
                    dto.name.trim(),
                    dto.address != null ? dto.address.trim() : "",
                    dto.lat,
                    dto.lon,
                    dto.active,
                    id);

            if (rows == 0) {
                log.warn("updateWarehouse: warehouse not found id={}", id);
                throw new RuntimeException("Warehouse not found: " + id);
            }
            log.info("updateWarehouse: updated warehouse id={}", id);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            log.error("updateWarehouse failed: id={}, error={}", id, e.getMessage(), e);
            throw e;
        }
    }

    // ── DELETE (soft) ─────────────────────────────────────────────────────────

    public void deactivateWarehouse(Long id) {
        log.info("deactivateWarehouse: id={}", id);
        try {
            int rows = jdbcTemplate.update(
                    "UPDATE warehouse_master SET active = false WHERE id = ?", id);
            if (rows == 0) {
                log.warn("deactivateWarehouse: warehouse not found id={}", id);
                throw new RuntimeException("Warehouse not found: " + id);
            }
            log.info("deactivateWarehouse: deactivated warehouse id={}", id);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            log.error("deactivateWarehouse failed: id={}, error={}", id, e.getMessage(), e);
            throw e;
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private WarehouseDTO mapRow(java.sql.ResultSet rs, int rowNum) throws java.sql.SQLException {
        WarehouseDTO d = new WarehouseDTO();
        d.id      = rs.getLong("id");
        d.name    = rs.getString("name");
        d.address = rs.getString("address");
        d.lat     = rs.getDouble("lat");
        d.lon     = rs.getDouble("lon");
        d.active  = rs.getBoolean("active");
        return d;
    }

    private void validate(WarehouseDTO dto) {
        if (dto.name == null || dto.name.isBlank())
            throw new IllegalArgumentException("Warehouse name is required");
        if (dto.lat == null || dto.lat < -90  || dto.lat > 90)
            throw new IllegalArgumentException("Valid latitude is required (-90 to 90)");
        if (dto.lon == null || dto.lon < -180 || dto.lon > 180)
            throw new IllegalArgumentException("Valid longitude is required (-180 to 180)");
    }

    // ── Static fallback ───────────────────────────────────────────────────────

    private List<WarehouseDTO> staticFallback() {
        WarehouseDTO w1 = new WarehouseDTO();
        w1.id = 1L; w1.name = "Main Warehouse";
        w1.address = "Mancheswar Industrial Estate, Bhubaneswar";
        w1.lat = 20.2827; w1.lon = 85.8679; w1.active = true;

        WarehouseDTO w2 = new WarehouseDTO();
        w2.id = 2L; w2.name = "North Hub";
        w2.address = "Patia, Bhubaneswar";
        w2.lat = 20.3526; w2.lon = 85.8194; w2.active = true;

        WarehouseDTO w3 = new WarehouseDTO();
        w3.id = 3L; w3.name = "South Depot";
        w3.address = "Jagamara, Bhubaneswar";
        w3.lat = 20.2349; w3.lon = 85.8173; w3.active = true;

        return List.of(w1, w2, w3);
    }
}
