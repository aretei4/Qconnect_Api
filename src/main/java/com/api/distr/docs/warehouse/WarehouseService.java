package com.api.distr.docs.warehouse;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;

import java.sql.PreparedStatement;
import java.sql.Statement;

/**
 * CRUD service for warehouse_master.
 *
 * DDL (run once):
 *   CREATE TABLE warehouse_master (
 *       id      BIGSERIAL PRIMARY KEY,
 *       name    VARCHAR(100) NOT NULL,
 *       address TEXT,
 *       lat     DOUBLE PRECISION,
 *       lon     DOUBLE PRECISION,
 *       active  BOOLEAN DEFAULT TRUE
 *   );
 */
@Service
public class WarehouseService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // ── READ ──────────────────────────────────────────────────────────────────

    /** Returns all active warehouses that have valid coordinates. */
    public List<WarehouseDTO> getActiveWarehouses() {
        try {
            return jdbcTemplate.query("""
                    SELECT id, name, address, lat, lon, active
                    FROM warehouse_master
                    WHERE active = true
                      AND lat IS NOT NULL
                      AND lon IS NOT NULL
                    ORDER BY id
                    """, this::mapRow);
        } catch (Exception e) {
            return staticFallback();
        }
    }

    /** Returns all warehouses (active + inactive) — for management screen. */
    public List<WarehouseDTO> getAllWarehouses() {
        try {
            return jdbcTemplate.query("""
                    SELECT id, name, address, lat, lon, active
                    FROM warehouse_master
                    ORDER BY id
                    """, this::mapRow);
        } catch (Exception e) {
            return staticFallback();
        }
    }

    // ── CREATE ────────────────────────────────────────────────────────────────

    public WarehouseDTO createWarehouse(WarehouseDTO dto) {
        validate(dto);
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
        return dto;
    }

    // ── UPDATE ────────────────────────────────────────────────────────────────

    public void updateWarehouse(Long id, WarehouseDTO dto) {
        validate(dto);
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

        if (rows == 0) throw new RuntimeException("Warehouse not found: " + id);
    }

    // ── DELETE (soft) ─────────────────────────────────────────────────────────

    public void deactivateWarehouse(Long id) {
        int rows = jdbcTemplate.update(
                "UPDATE warehouse_master SET active = false WHERE id = ?", id);
        if (rows == 0) throw new RuntimeException("Warehouse not found: " + id);
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
