package com.api.distr.docs.dayend;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class DayEndApprovalService {

    private static final Logger log = LoggerFactory.getLogger(DayEndApprovalService.class);

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * Ensures dayend_approval has all columns required by create/approve/reject flows.
     * Uses IF NOT EXISTS so it is safe to run on every startup.
     */
    @PostConstruct
    public void ensureSchema() {
        String[][] columns = {
            {"total_amount",  "NUMERIC(15,2) DEFAULT 0"},
            {"reject_reason", "TEXT"},
            {"approved_at",   "TIMESTAMP"},
            {"request_date",  "TIMESTAMP"},
            {"start_time",    "TIMESTAMP"},
            {"status",        "VARCHAR(20) DEFAULT 'STARTED'"},
            {"picklist_nos",  "TEXT"}
        };
        for (String[] col : columns) {
            try {
                jdbcTemplate.execute(
                    "ALTER TABLE dayend_approval ADD COLUMN IF NOT EXISTS " + col[0] + " " + col[1]);
            } catch (Exception e) {
                log.warn("ensureSchema: could not add column {} — {}", col[0], e.getMessage());
            }
        }
        log.info("ensureSchema: dayend_approval columns verified");
    }

    // ✅ Date Parser
    private LocalDate parseDate(String date) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
        return LocalDate.parse(date, formatter);
    }

    // ✅ START — agent signals day-end has begun (status = STARTED)
    public void startDayEnd(DayEndDto dto) {

        if (dto.getDate() == null || dto.getDate().isBlank())
            throw new IllegalArgumentException("date is required");
        if (dto.getDeliveryId() == null)
            throw new IllegalArgumentException("deliveryId is required");

        LocalDate date = parseDate(dto.getDate());
        log.info("startDayEnd: deliveryId={}, date={}", dto.getDeliveryId(), date);

        try {
            String checkSql = "SELECT COUNT(*) FROM dayend_approval WHERE delivery_id = ? AND delivery_date = ?";
            Integer count = jdbcTemplate.queryForObject(checkSql, Integer.class, dto.getDeliveryId(), date);

            if (count != null && count > 0) {
                log.info("startDayEnd: resetting existing record to STARTED for deliveryId={}, date={}", dto.getDeliveryId(), date);
                jdbcTemplate.update(
                    "UPDATE dayend_approval " +
                    "SET status = 'STARTED', start_time = NOW(), total_amount = 0, " +
                    "    reject_reason = NULL, approved_at = NULL, request_date = NULL " +
                    "WHERE delivery_id = ? AND delivery_date = ?",
                    dto.getDeliveryId(), date);
            } else {
                log.info("startDayEnd: inserting new STARTED record for deliveryId={}, date={}", dto.getDeliveryId(), date);
                String insertSql = """
                    INSERT INTO dayend_approval
                    (delivery_id, delivery_date, start_time, status, total_amount)
                    VALUES (?, ?, NOW(), 'STARTED', 0)
                """;
                jdbcTemplate.update(insertSql, dto.getDeliveryId(), date);
            }
        } catch (Exception e) {
            log.error("startDayEnd failed: deliveryId={}, date={}, error={}", dto.getDeliveryId(), date, e.getMessage(), e);
            throw e;
        }
    }

    // ✅ CREATE
    public void createDayEnd(DayEndDto dto) {

        if (dto.getDate() == null || dto.getDate().isBlank())
            throw new IllegalArgumentException("date is required");
        if (dto.getDeliveryId() == null)
            throw new IllegalArgumentException("deliveryId is required");

        LocalDate date        = parseDate(dto.getDate());
        double    totalAmt    = dto.getTotalAmount() != null ? dto.getTotalAmount() : 0.0;
        List<String> nos      = dto.getPicklistNos();
        String picklistNosStr = (nos != null && !nos.isEmpty())
                                ? String.join(",", nos)
                                : null;

        log.info("createDayEnd: deliveryId={}, date={}, totalAmount={}, picklists={}",
                dto.getDeliveryId(), date, totalAmt, picklistNosStr);

        try {
            // ── Guard 1: block if a PENDING approval already exists for this agent/date ──
            try {
                String statusSql =
                    "SELECT status FROM dayend_approval WHERE delivery_id = ? AND delivery_date = ? LIMIT 1";
                String currentStatus = jdbcTemplate.queryForObject(
                    statusSql, String.class, dto.getDeliveryId(), date);
                if ("PENDING".equals(currentStatus)) {
                    throw new IllegalStateException(
                        "Day end for this agent is already submitted and awaiting admin approval. " +
                        "Please wait for the admin to approve or reject before re-submitting.");
                }
            } catch (EmptyResultDataAccessException ignored) {
                // no record yet — allowed to proceed
            }

            // ── Guard 2: block if any delivery items are still pending (not settled) ──
            // Pending = in delivery_assignments with status=0 AND no corresponding delivery_status row
            String pendingSql = """
                SELECT COUNT(*)
                FROM delivery_assignments da
                WHERE da.delivery_boy_id = ?
                  AND da.status = 0
                  AND NOT EXISTS (
                      SELECT 1 FROM delivery_status ds WHERE ds.dire_id = da.dire_id
                  )
                """;
            Integer pendingCount = jdbcTemplate.queryForObject(
                pendingSql, Integer.class, String.valueOf(dto.getDeliveryId()));
            if (pendingCount != null && pendingCount > 0) {
                log.warn("createDayEnd: {} item(s) still pending for deliveryId={}, date={}",
                    pendingCount, dto.getDeliveryId(), date);
                throw new IllegalStateException(
                    pendingCount + " delivery item" + (pendingCount > 1 ? "s are" : " is") +
                    " still pending. Please mark all deliveries as delivered or not-delivered " +
                    "before closing the day.");
            }

            String checkSql = "SELECT COUNT(*) FROM dayend_approval WHERE delivery_id = ? AND delivery_date = ?";
            Integer count   = jdbcTemplate.queryForObject(checkSql, Integer.class, dto.getDeliveryId(), date);

            if (count != null && count > 0) {
                log.info("createDayEnd: updating existing record to PENDING for deliveryId={}", dto.getDeliveryId());
                jdbcTemplate.update(
                    "UPDATE dayend_approval " +
                    "SET status = 'PENDING', request_date = NOW(), total_amount = ?, picklist_nos = ?, " +
                    "    reject_reason = NULL, approved_at = NULL " +
                    "WHERE delivery_id = ? AND delivery_date = ?",
                    totalAmt, picklistNosStr, dto.getDeliveryId(), date);
            } else {
                log.info("createDayEnd: no STARTED row found — inserting new PENDING record for deliveryId={}", dto.getDeliveryId());
                jdbcTemplate.update(
                    "INSERT INTO dayend_approval " +
                    "(delivery_id, delivery_date, request_date, status, total_amount, picklist_nos) " +
                    "VALUES (?, ?, NOW(), 'PENDING', ?, ?)",
                    dto.getDeliveryId(), date, totalAmt, picklistNosStr);
            }

        } catch (Exception e) {
            log.error("createDayEnd failed: deliveryId={}, date={}, error={}", dto.getDeliveryId(), date, e.getMessage(), e);
            throw e;
        }
    }

    // ✅ APPROVE BY ID
    public void approveDayEndById(DayEndDto dto) {
        if (dto.getDayendId() == null)
            throw new RuntimeException("dayendId is required");

        log.info("approveDayEndById: dayendId={}", dto.getDayendId());
        try {
            int updated = jdbcTemplate.update("""
                UPDATE dayend_approval
                SET status = 'APPROVED',
                    approved_at = NOW(),
                    reject_reason = NULL
                WHERE id = ?
                  AND status = 'PENDING'
            """, dto.getDayendId());

            if (updated == 0) {
                log.warn("approveDayEndById: no PENDING record found for dayendId={}", dto.getDayendId());
                throw new RuntimeException("No pending DayEnd found to approve");
            }
            log.info("approveDayEndById: approved dayendId={}", dto.getDayendId());
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            log.error("approveDayEndById failed: dayendId={}, error={}", dto.getDayendId(), e.getMessage(), e);
            throw e;
        }
    }

    // ❌ REJECT BY ID
    public void rejectDayEndById(DayEndDto dto) {
        if (dto.getDayendId() == null)
            throw new RuntimeException("dayendId is required");
        if (dto.getRejectReason() == null || dto.getRejectReason().isBlank())
            throw new RuntimeException("Reject reason is required");

        log.info("rejectDayEndById: dayendId={}, reason={}", dto.getDayendId(), dto.getRejectReason());
        try {
            int updated = jdbcTemplate.update("""
                UPDATE dayend_approval
                SET status = 'REJECTED',
                    reject_reason = ?,
                    approved_at = NULL
                WHERE id = ?
                  AND status = 'PENDING'
            """, dto.getRejectReason(), dto.getDayendId());

            if (updated == 0) {
                log.warn("rejectDayEndById: no PENDING record found for dayendId={}", dto.getDayendId());
                throw new RuntimeException("No pending DayEnd found to reject");
            }
            log.info("rejectDayEndById: rejected dayendId={}", dto.getDayendId());
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            log.error("rejectDayEndById failed: dayendId={}, error={}", dto.getDayendId(), e.getMessage(), e);
            throw e;
        }
    }

    // 🟢 ONLINE AGENTS — agents active today (STARTED or PENDING)
    public List<OnlineAgentDto> getOnlineAgents() {
        log.info("getOnlineAgents: querying for today={}", LocalDate.now());
        try {
            String sql = """
                SELECT
                    da.delivery_id,
                    dm.delivery_name                                                   AS delivery_boy_name,
                    da.status,
                    da.start_time,
                    da.total_amount,
                    COALESCE((
                        SELECT COUNT(*) FROM delivery_status ds
                        WHERE ds.delivery_id = da.delivery_id
                          AND ds.delivery_date::date = CURRENT_DATE
                    ), 0)                                                              AS total_deliveries,
                    COALESCE((
                        SELECT COUNT(*) FROM delivery_status ds
                        WHERE ds.delivery_id = da.delivery_id
                          AND ds.delivery_date::date = CURRENT_DATE
                          AND ds.delivered = true
                    ), 0)                                                              AS delivered_count
                FROM dayend_approval da
                LEFT JOIN delivery_master dm ON da.delivery_id = dm.delivery_id
                WHERE da.delivery_date = CURRENT_DATE
                  AND da.status IN ('STARTED', 'PENDING')
                ORDER BY da.start_time DESC
            """;

            List<OnlineAgentDto> result = jdbcTemplate.query(sql, (rs, rowNum) -> {
                OnlineAgentDto dto = new OnlineAgentDto();
                dto.setDeliveryId(rs.getLong("delivery_id"));
                dto.setDeliveryBoyName(rs.getString("delivery_boy_name"));
                dto.setStatus(rs.getString("status"));
                dto.setTotalDeliveries(rs.getInt("total_deliveries"));
                dto.setDeliveredCount(rs.getInt("delivered_count"));
                dto.setTotalAmount(rs.getDouble("total_amount"));
                if (rs.getTimestamp("start_time") != null)
                    dto.setStartTime(rs.getTimestamp("start_time").toLocalDateTime());
                return dto;
            });

            log.info("getOnlineAgents: found {} active agents", result.size());
            return result;
        } catch (Exception e) {
            log.error("getOnlineAgents failed: {}", e.getMessage(), e);
            throw e;
        }
    }

    // 🔍 LIST (UNCHANGED)
    public List<DayEndResponseDto> getDayEndList(
            Long deliveryId,
            LocalDate fromDate,
            LocalDate toDate
    ) {

        StringBuilder sql = new StringBuilder(
            "SELECT d.id, d.delivery_id, dm.delivery_name AS delivery_boy_name, " +
            "       d.delivery_date, d.status, d.total_amount, d.reject_reason, " +
            "       d.start_time, d.request_date, d.approved_at " +
            "FROM dayend_approval d " +
            "LEFT JOIN delivery_master dm ON d.delivery_id = dm.delivery_id " +
            "WHERE d.delivery_date BETWEEN ? AND ?");

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
                            rs.getTimestamp("request_date") != null
                                    ? rs.getTimestamp("request_date").toLocalDateTime()
                                    : null
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

                    return res;
                }
        );
    }
}