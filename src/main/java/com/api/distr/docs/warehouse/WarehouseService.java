package com.api.distr.docs.warehouse;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class WarehouseService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    public List<WarehouseDTO> getActiveWarehouses() {
        try {
            String sql = """
                    SELECT id, name, address, lat, lon, active
                    FROM warehouse_master
                    WHERE active = true
                      AND lat IS NOT NULL
                      AND lon IS NOT NULL
                    ORDER BY id
                    """;

            return jdbcTemplate.query(sql, (rs, rowNum) -> {
                WarehouseDTO dto = new WarehouseDTO();
                dto.id      = rs.getLong("id");
                dto.name    = rs.getString("name");
                dto.address = rs.getString("address");
                dto.lat     = rs.getDouble("lat");
                dto.lon     = rs.getDouble("lon");
                dto.active  = rs.getBoolean("active");
                return dto;
            });
        } catch (Exception e) {
            // Table not yet created — return static fallback
            return staticFallback();
        }
    }

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
