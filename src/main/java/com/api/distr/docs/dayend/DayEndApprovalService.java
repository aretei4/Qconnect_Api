package com.api.distr.docs.dayend;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class DayEndApprovalService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // ✅ Date Parser
    private LocalDate parseDate(String date) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
        return LocalDate.parse(date, formatter);
    }

    // ✅ START — agent signals day-end has begun (status = STARTED)
    public void startDayEnd(DayEndDto dto) {

        LocalDate date = parseDate(dto.getDate());

        String checkSql = "SELECT COUNT(*) FROM dayend_approval WHERE delivery_id = ? AND delivery_date = ?";
        Integer count = jdbcTemplate.queryForObject(checkSql, Integer.class, dto.getDeliveryId(), date);

        if (count != null && count > 0) {
            // Reset existing record back to STARTED
            String updateSql = """
                UPDATE dayend_approval
                SET status     = 'STARTED',
                    start_time = NOW(),
                    total_amount   = 0,
                    picklist_nos   = NULL,
                    reject_reason  = NULL,
                    approved_at    = NULL,
                    request_date   = NULL
                WHERE delivery_id = ? AND delivery_date = ?
            """;
            jdbcTemplate.update(updateSql, dto.getDeliveryId(), date);
        } else {
            String insertSql = """
                INSERT INTO dayend_approval
                (delivery_id, delivery_date, start_time, status, total_amount)
                VALUES (?, ?, NOW(), 'STARTED', 0)
            """;
            jdbcTemplate.update(insertSql, dto.getDeliveryId(), date);
        }
    }

    // ✅ CREATE
    public void createDayEnd(DayEndDto dto) {

        LocalDate date = parseDate(dto.getDate());

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

        // Convert List<String> → "E587P001,E587P002"
        String picklistNosStr = (dto.getPicklistNos() != null && !dto.getPicklistNos().isEmpty())
                ? String.join(",", dto.getPicklistNos())
                : null;

        if (count != null && count > 0) {
            // Record exists — reset it to PENDING so it can be re-submitted
            String updateSql = """
                UPDATE dayend_approval
                SET status = 'PENDING',
                    request_date = NOW(),
                    total_amount = ?,
                    picklist_nos = ?,
                    reject_reason = NULL,
                    approved_at = NULL
                WHERE delivery_id = ? AND delivery_date = ?
            """;
            jdbcTemplate.update(
                    updateSql,
                    dto.getTotalAmount() != null ? dto.getTotalAmount() : 0.0,
                    picklistNosStr,
                    dto.getDeliveryId(),
                    date
            );
            return;
        }

        String insertSql = """
            INSERT INTO dayend_approval
            (delivery_id, delivery_date, request_date, status, total_amount, picklist_nos)
            VALUES (?, ?, NOW(), 'PENDING', ?, ?)
        """;

        jdbcTemplate.update(
                insertSql,
                dto.getDeliveryId(),
                date,
                dto.getTotalAmount() != null ? dto.getTotalAmount() : 0.0,
                picklistNosStr
        );
    }

    // ✅ APPROVE BY ID
    public void approveDayEndById(DayEndDto dto) {

        if (dto.getDayendId() == null) {
            throw new RuntimeException("dayendId is required");
        }

        String sql = """
            UPDATE dayend_approval
            SET status = 'APPROVED',
                approved_at = NOW(),
                reject_reason = NULL
            WHERE id = ?
              AND status = 'PENDING'
        """;

        int updated = jdbcTemplate.update(sql, dto.getDayendId());

        if (updated == 0) {
            throw new RuntimeException("No pending DayEnd found to approve");
        }
    }

    // ❌ REJECT BY ID
    public void rejectDayEndById(DayEndDto dto) {

        if (dto.getDayendId() == null) {
            throw new RuntimeException("dayendId is required");
        }

        if (dto.getRejectReason() == null || dto.getRejectReason().isBlank()) {
            throw new RuntimeException("Reject reason is required");
        }

        String sql = """
            UPDATE dayend_approval
            SET status = 'REJECTED',
                reject_reason = ?,
                approved_at = NULL
            WHERE id = ?
              AND status = 'PENDING'
        """;

        int updated = jdbcTemplate.update(
                sql,
                dto.getRejectReason(),
                dto.getDayendId()
        );

        if (updated == 0) {
            throw new RuntimeException("No pending DayEnd found to reject");
        }
    }

    // 🔍 LIST (UNCHANGED)
    public List<DayEndResponseDto> getDayEndList(
            Long deliveryId,
            LocalDate fromDate,
            LocalDate toDate
    ) {

        StringBuilder sql = new StringBuilder("""
            SELECT
                d.id,
                d.delivery_id,
                dm.delivery_name AS delivery_boy_name,
                d.delivery_date,
                d.status,
                d.total_amount,
                d.reject_reason,
                d.start_time,
                d.request_date,
                d.approved_at,
                d.picklist_nos
            FROM dayend_approval d
            LEFT JOIN delivery_master dm
                   ON d.delivery_id = dm.delivery_id
            WHERE d.delivery_date BETWEEN ? AND ?
        """);

        List<Object> params = new ArrayList<>();
        params.add(fromDate);
        params.add(toDate);

        if (deliveryId != null) {
            sql.append(" AND d.delivery_id = ?");
            params.add(deliveryId);
        }

        sql.append(" ORDER BY d.delivery_date DESC");

        return jdbcTemplate.query(
                sql.toString(),
                params.toArray(),
                (rs, rowNum) -> {

                    DayEndResponseDto res = new DayEndResponseDto();
                    res.setDayendId(rs.getLong("id"));
                    res.setDeliveryId(rs.getLong("delivery_id"));
                    res.setDeliveryBoyName(rs.getString("delivery_boy_name"));
                    res.setDeliveryDate(rs.getDate("delivery_date").toLocalDate());
                    res.setStatus(rs.getString("status"));
                    res.setTotalAmount(rs.getDouble("total_amount"));
                    res.setRejectReason(rs.getString("reject_reason"));

                    res.setRequestDate(
                            rs.getTimestamp("request_date").toLocalDateTime()
                    );

                    res.setStartTime(
                            rs.getTimestamp("start_time") != null
                                    ? rs.getTimestamp("start_time").toLocalDateTime()
                                    : null
                    );

                    res.setApprovedAt(
                            rs.getTimestamp("approved_at") != null
                                    ? rs.getTimestamp("approved_at").toLocalDateTime()
                                    : null
                    );

                    // Parse "E587P001,E587P002,E587P003" → List<String>
                    String rawPicklists = rs.getString("picklist_nos");
                    if (rawPicklists != null && !rawPicklists.isBlank()) {
                        res.setPicklistNos(Arrays.asList(rawPicklists.split(",")));
                    } else {
                        res.setPicklistNos(Collections.emptyList());
                    }

                    return res;
                }
        );
    }
}