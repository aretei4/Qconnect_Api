package com.api.distr.docs.upload;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class DeliveryExcelRepository {

    @Autowired
    private JdbcTemplate jdbc;

    public void saveOrUpdate(DeliveryExcelDTO dto) {
        if (countByMobile(dto.getDeliveryMobile()) > 0) {
            update(dto);
        } else {
            insert(dto);
        }
    }

    public int countByMobile(String mobile) {
        return jdbc.queryForObject(
            "SELECT COUNT(*) FROM delivery_master WHERE delivery_mobile = ?",
            Integer.class, mobile
        );
    }

    public void insert(DeliveryExcelDTO dto) {
        String sql = """
            INSERT INTO delivery_master (
                delivery_name, delivery_mobile, alt_mobile,
                updated_date, active, bu_id,
                lat, lon, address,
                address1, address2, address3,
                city, pin_code,
                father_name, aadhar_no, pan_card, bank_account,
                date_of_joining, type
            ) VALUES (
                ?, ?, ?,
                ?, true, 100,
                ?, ?, ?,
                ?, ?, ?,
                ?, ?,
                ?, ?, ?, ?,
                ?, ?
            )
            """;

        jdbc.update(conn -> {
            var ps = conn.prepareStatement(sql);
            ps.setString(1,  dto.getDeliveryName());
            ps.setString(2,  dto.getDeliveryMobile());
            ps.setString(3,  dto.getAltMobile());
            ps.setDate  (4,  dto.getUpdatedDate());
            ps.setObject(5,  dto.getLat());
            ps.setObject(6,  dto.getLon());
            ps.setString(7,  dto.getAddress());
            ps.setString(8,  dto.getAddress1());
            ps.setString(9,  dto.getAddress2());
            ps.setString(10, dto.getAddress3());
            ps.setString(11, dto.getCity());
            ps.setString(12, dto.getPinCode());
            ps.setString(13, dto.getFatherName());
            ps.setString(14, dto.getAadharNo());
            ps.setString(15, dto.getPanCard());
            ps.setString(16, dto.getBankAccount());
            ps.setDate  (17, dto.getDateOfJoining());
            ps.setString(18, dto.getType());
            return ps;
        });
    }

    public void update(DeliveryExcelDTO dto) {
        String sql = """
            UPDATE delivery_master SET
                delivery_name   = ?,
                alt_mobile      = ?,
                updated_date    = ?,
                address         = ?,
                address1        = ?,
                address2        = ?,
                address3        = ?,
                city            = ?,
                pin_code        = ?,
                father_name     = ?,
                aadhar_no       = ?,
                pan_card        = ?,
                bank_account    = ?,
                date_of_joining = ?
            WHERE delivery_mobile = ?
            """;

        jdbc.update(conn -> {
            var ps = conn.prepareStatement(sql);
            ps.setString(1,  dto.getDeliveryName());
            ps.setString(2,  dto.getAltMobile());
            ps.setDate  (3,  dto.getUpdatedDate());
            ps.setString(4,  dto.getAddress());
            ps.setString(5,  dto.getAddress1());
            ps.setString(6,  dto.getAddress2());
            ps.setString(7,  dto.getAddress3());
            ps.setString(8,  dto.getCity());
            ps.setString(9,  dto.getPinCode());
            ps.setString(10, dto.getFatherName());
            ps.setString(11, dto.getAadharNo());
            ps.setString(12, dto.getPanCard());
            ps.setString(13, dto.getBankAccount());
            ps.setDate  (14, dto.getDateOfJoining());
            ps.setString(15, dto.getDeliveryMobile());
            return ps;
        });
    }
}
