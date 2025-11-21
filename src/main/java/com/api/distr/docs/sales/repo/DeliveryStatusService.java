package com.api.distr.docs.sales.repo;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import com.api.distr.docs.sales.dto.DeliveryStatusDTO;

@Service
public class DeliveryStatusService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    public List<DeliveryStatusDTO> getByUpdateDate(LocalDate fromDate, LocalDate toDate) {

        String sql = """
            SELECT delivery_id, picklist_no, delivered, otp, 
                   payment_amount, payment_mode, reason, updadate_date
            FROM delivery_status
            WHERE updadate_date BETWEEN ? AND ?
            ORDER BY updadate_date DESC
        """;

        return jdbcTemplate.query(sql, new Object[]{
                fromDate.atStartOfDay(),
                toDate.atTime(23, 59, 59)
        }, (rs, rowNum) -> {

            DeliveryStatusDTO dto = new DeliveryStatusDTO();

            dto.delivery_id = rs.getString("delivery_id");
            dto.picklist_no = rs.getString("picklist_no");
            dto.delivered = rs.getBoolean("delivered");
            dto.otp = rs.getBoolean("otp");
            dto.payment_amount = rs.getDouble("payment_amount");
            dto.payment_mode = rs.getString("payment_mode");
            dto.reason = rs.getString("reason");

            // Format date into dd/MM/yyyy
            LocalDateTime date = rs.getTimestamp("updadate_date").toLocalDateTime();
            dto.delivery_date = date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));

            return dto;
        });
    }
}

