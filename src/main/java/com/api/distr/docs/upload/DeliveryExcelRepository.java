package com.api.distr.docs.upload;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class DeliveryExcelRepository {

    @Autowired
    private JdbcTemplate jdbc;

    public int countByMobile(String mobile) {
        String sql = "SELECT COUNT(*) FROM delivery_master WHERE delivery_mobile=?";
        return jdbc.queryForObject(sql, Integer.class, mobile);
    }

    public void insert(DeliveryExcelDTO dto) {
        String sql = """
            INSERT INTO delivery_master (
                delivery_name, delivery_mobile, updated_date, active, bu_id, lat, lon, address
            )
            VALUES (?, ?, ?, true, 100, ?, ?, ?)
            """;

        jdbc.update(conn -> {
            var ps = conn.prepareStatement(sql);
            ps.setString(1, dto.getDeliveryName());
            ps.setString(2, dto.getDeliveryMobile());
            ps.setDate(3, dto.getUpdatedDate());
            ps.setObject(4, dto.getLat());
            ps.setObject(5, dto.getLon());
            ps.setString(6, dto.getAddress());
            return ps;
        });
    }

    public void update(DeliveryExcelDTO dto) {
        String sql = """
            UPDATE delivery_master
            SET delivery_name=?, updated_date=?, lat=?, lon=?, address=?
            WHERE delivery_mobile=?
            """;

        jdbc.update(conn -> {
            var ps = conn.prepareStatement(sql);
            ps.setString(1, dto.getDeliveryName());
            ps.setDate(2, dto.getUpdatedDate());
            ps.setObject(3, dto.getLat());
            ps.setObject(4, dto.getLon());
            ps.setString(5, dto.getAddress());
            ps.setString(6, dto.getDeliveryMobile());
            return ps;
        });
    }
}

