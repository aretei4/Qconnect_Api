package com.api.distr.docs.dayend;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class DayEndApprovalService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // ✅ Common Date Parser
    private LocalDate parseDate(String date) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
        return LocalDate.parse(date, formatter);
    }

    // ✅ CREATE (PENDING)
    public void createDayEnd(DayEndDto dto) {

        LocalDate date = parseDate(dto.getDate());

        // 🔒 Prevent duplicate
        String checkSql = """
            SELECT COUNT(*) FROM dayend_approval
            WHERE delivery_id = ? AND delivery_date = ?
        """;

        Integer count = jdbcTemplate.queryForObject(
                checkSql,
                Integer.class,
                dto.getDeliveryId(),
                date
        );

        if (count != null && count > 0) {
            throw new RuntimeException("DayEnd already exists for this delivery & date");
        }

        String insertSql = """
            INSERT INTO dayend_approval
            (delivery_id, delivery_date, request_date, status, total_amount)
            VALUES (?, ?, NOW(), 'PENDING', ?)
        """;

        jdbcTemplate.update(
                insertSql,
                dto.getDeliveryId(),
                date,
                dto.getTotalAmount() != null ? dto.getTotalAmount() : 0.0
        );
    }

    // ✅ APPROVE
    public void approveDayEnd(DayEndDto dto) {

        LocalDate date = parseDate(dto.getDate());

        String updateSql = """
            UPDATE dayend_approval
            SET status = 'APPROVED',
                approved_at = NOW(),
                reject_reason = NULL
            WHERE delivery_id = ?
              AND delivery_date = ?
              AND status = 'PENDING'
        """;

        int updated = jdbcTemplate.update(
                updateSql,
                dto.getDeliveryId(),
                date
        );

        if (updated == 0) {
            throw new RuntimeException("No pending DayEnd found to approve");
        }
    }

    // ❌ REJECT
    public void rejectDayEnd(DayEndDto dto) {

        LocalDate date = parseDate(dto.getDate());

        if (dto.getRejectReason() == null || dto.getRejectReason().isBlank()) {
            throw new RuntimeException("Reject reason is required");
        }

        String updateSql = """
            UPDATE dayend_approval
            SET status = 'REJECTED',
                reject_reason = ?,
                approved_at = NULL
            WHERE delivery_id = ?
              AND delivery_date = ?
              AND status = 'PENDING'
        """;

        int updated = jdbcTemplate.update(
                updateSql,
                dto.getRejectReason(),
                dto.getDeliveryId(),
                date
        );

        if (updated == 0) {
            throw new RuntimeException("No pending DayEnd found to reject");
        }
    }

    // 🔍 GET
    public DayEndResponseDto getDayEnd(DayEndDto dto) {

        LocalDate date = parseDate(dto.getDate());

        String sql = """
            SELECT d.delivery_id,
                   dm.delivery_name AS delivery_boy_name,
                   d.delivery_date,
                   d.status,
                   d.total_amount,
                   d.reject_reason,
                   d.request_date,
                   d.approved_at
            FROM dayend_approval d
            LEFT JOIN delivery_master dm
                   ON d.delivery_id = dm.delivery_id
            WHERE d.delivery_id = ?
              AND d.delivery_date = ?
        """;

        return jdbcTemplate.queryForObject(
                sql,
                (rs, rowNum) -> {

                    DayEndResponseDto res = new DayEndResponseDto();

                    res.setDeliveryId(rs.getLong("delivery_id"));
                    res.setDeliveryBoyName(rs.getString("delivery_boy_name")); // 🆕
                    res.setDeliveryDate(rs.getDate("delivery_date").toLocalDate());
                    res.setStatus(rs.getString("status"));
                    res.setTotalAmount(rs.getDouble("total_amount"));
                    res.setRejectReason(rs.getString("reject_reason"));

                    res.setRequestDate(
                            rs.getTimestamp("request_date").toLocalDateTime()
                    );

                    res.setApprovedAt(
                            rs.getTimestamp("approved_at") != null
                                    ? rs.getTimestamp("approved_at").toLocalDateTime()
                                    : null
                    );

                    return res;
                },
                dto.getDeliveryId(),
                date
        );
    }
}
