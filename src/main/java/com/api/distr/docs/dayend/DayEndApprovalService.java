package com.api.distr.docs.dayend;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

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

    /** Who/when for each approval stage is recorded here (replaced the *_approved_at columns). */
    @Autowired
    private com.api.distr.docs.dan.DanApprovalRepository approvalRepository;

    /** The agent's most recent dayend_approval id — the DAN the lifecycle event belongs to. */
    private Long openDanId(Long deliveryId) {
        try {
            return jdbcTemplate.queryForObject(
                    "SELECT id FROM dayend_approval WHERE delivery_id = ? ORDER BY id DESC LIMIT 1",
                    Long.class, deliveryId);
        } catch (Exception e) {
            log.warn("openDanId: could not resolve DAN for deliveryId={}", deliveryId);
            return null;
        }
    }

    /** Authenticated username, or "system" when unauthenticated. */
    private String currentUser() {
        try {
            var auth = org.springframework.security.core.context.SecurityContextHolder
                    .getContext().getAuthentication();
            if (auth != null
                    && !(auth instanceof org.springframework.security.authentication.AnonymousAuthenticationToken)
                    && auth.getName() != null && !auth.getName().isBlank()) {
                return auth.getName();
            }
        } catch (Exception ignored) { }
        return "system";
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
            // "open" statuses = STARTED, PENDING, REJECTED
            final String OPEN = "(" + DayEndStatus.STARTED + "," + DayEndStatus.PENDING + "," + DayEndStatus.REJECTED + ")";
            String checkSql = "SELECT COUNT(*) FROM dayend_approval WHERE delivery_id = ? AND status IN " + OPEN;
            Integer count = jdbcTemplate.queryForObject(checkSql, Integer.class, dto.getDeliveryId());

            if (count != null && count > 0) {
                log.info("startDayEnd: resetting existing open record to STARTED for deliveryId={}", dto.getDeliveryId());
                jdbcTemplate.update(
                    "UPDATE dayend_approval " +
                    "SET status = " + DayEndStatus.STARTED + ", start_time = NOW(), total_amount = 0, " +
                    "    reject_reason = NULL, approved_at = NULL, request_date = NULL " +
                    "WHERE delivery_id = ? AND status IN " + OPEN,
                    dto.getDeliveryId());
            } else {
                log.info("startDayEnd: inserting new STARTED record for deliveryId={}, date={}", dto.getDeliveryId(), date);
                String insertSql =
                    "INSERT INTO dayend_approval " +
                    "(delivery_id, delivery_date, start_time, status, total_amount) " +
                    "VALUES (?, ?, NOW(), " + DayEndStatus.STARTED + ", 0)";
                jdbcTemplate.update(insertSql, dto.getDeliveryId(), date);
            }

            // Log the DAN-created event against the (possibly just-inserted) open record
            approvalRepository.logEvent(openDanId(dto.getDeliveryId()), "STARTED", "CREATED",
                    currentUser(), "Day end started");

            // Move all ASSIGNED (9) rows for this agent to PENDING (0) so delivery can begin
            int moved = jdbcTemplate.update(
                "UPDATE delivery_assignments SET status = 0 " +
                "WHERE delivery_boy_id = ? AND status = 9",
                String.valueOf(dto.getDeliveryId()));
            log.info("startDayEnd: set {} assignment(s) from ASSIGNED→PENDING for deliveryId={}", moved, dto.getDeliveryId());
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
                    "SELECT status FROM dayend_approval WHERE delivery_id = ? ORDER BY id DESC LIMIT 1";
                Integer currentStatus = jdbcTemplate.queryForObject(
                    statusSql, Integer.class, dto.getDeliveryId());
                if (currentStatus != null
                        && (currentStatus == DayEndStatus.PENDING || currentStatus == DayEndStatus.SK_APPROVED)) {
                    throw new IllegalStateException(
                        "Day end for this agent is already submitted and awaiting approval. " +
                        "Please wait for the approval or rejection before re-submitting.");
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

            final String STARTED_OR_REJECTED = "(" + DayEndStatus.STARTED + "," + DayEndStatus.REJECTED + ")";
            String checkSql = "SELECT COUNT(*) FROM dayend_approval WHERE delivery_id = ? AND status IN " + STARTED_OR_REJECTED;
            Integer count   = jdbcTemplate.queryForObject(checkSql, Integer.class, dto.getDeliveryId());

            if (count != null && count > 0) {
                log.info("createDayEnd: updating existing record to PENDING for deliveryId={}", dto.getDeliveryId());
                jdbcTemplate.update(
                    "UPDATE dayend_approval " +
                    "SET status = " + DayEndStatus.PENDING + ", request_date = NOW(), total_amount = ?, picklist_nos = ?, " +
                    "    reject_reason = NULL, approved_at = NULL " +
                    "WHERE delivery_id = ? AND status IN " + STARTED_OR_REJECTED,
                    totalAmt, picklistNosStr, dto.getDeliveryId());
            } else {
                log.info("createDayEnd: no STARTED row found — inserting new PENDING record for deliveryId={}", dto.getDeliveryId());
                jdbcTemplate.update(
                    "INSERT INTO dayend_approval " +
                    "(delivery_id, delivery_date, request_date, status, total_amount, picklist_nos) " +
                    "VALUES (?, ?, NOW(), " + DayEndStatus.PENDING + ", ?, ?)",
                    dto.getDeliveryId(), date, totalAmt, picklistNosStr);
            }

            approvalRepository.logEvent(openDanId(dto.getDeliveryId()), "SUBMITTED", "CREATED",
                    currentUser(), "Submitted for approval — " + totalAmt);

        } catch (Exception e) {
            log.error("createDayEnd failed: deliveryId={}, date={}, error={}", dto.getDeliveryId(), date, e.getMessage(), e);
            throw e;
        }
    }

    // ✅ APPROVE BY ID — two-stage: STOREKEEPER (PENDING→SK_APPROVED) or ACCOUNTANT (SK_APPROVED/PENDING→APPROVED)
    public void approveDayEndById(DayEndDto dto) {
        if (dto.getDayendId() == null)
            throw new RuntimeException("dayendId is required");

        String role = dto.getApproverRole();
        log.info("approveDayEndById: dayendId={}, approverRole={}", dto.getDayendId(), role);
        try {
            int updated;
            if ("STOREKEEPER".equalsIgnoreCase(role)) {
                // Stage 1: PENDING → SK_APPROVED
                updated = jdbcTemplate.update(
                    "UPDATE dayend_approval " +
                    "SET status = " + DayEndStatus.SK_APPROVED + ", reject_reason = NULL " +
                    "WHERE id = ? AND status = " + DayEndStatus.PENDING,
                    dto.getDayendId());
                if (updated == 0) {
                    log.warn("approveDayEndById(SK): no PENDING record for dayendId={}", dto.getDayendId());
                    throw new RuntimeException("No pending DayEnd found for storekeeper approval");
                }
                // Who/when is captured in dan_approval_log (replaces storekeeper_approved_at)
                approvalRepository.insert(dto.getDayendId(), "STOREKEEPER", "APPROVED",
                        currentUser(), null, null);
                log.info("approveDayEndById(SK): SK_APPROVED dayendId={}", dto.getDayendId());
            } else {
                // Stage 2 (ACCOUNTANT / default): SK_APPROVED or PENDING → APPROVED
                updated = jdbcTemplate.update(
                    "UPDATE dayend_approval " +
                    "SET status = " + DayEndStatus.APPROVED + ", approved_at = NOW(), reject_reason = NULL " +
                    "WHERE id = ? AND status IN (" + DayEndStatus.PENDING + "," + DayEndStatus.SK_APPROVED + ")",
                    dto.getDayendId());
                if (updated == 0) {
                    log.warn("approveDayEndById(ACCT): no approvable record for dayendId={}", dto.getDayendId());
                    throw new RuntimeException("No approvable DayEnd found for accountant approval");
                }
                approvalRepository.insert(dto.getDayendId(), "ACCOUNTS", "APPROVED",
                        currentUser(), null, null);
                log.info("approveDayEndById(ACCT): APPROVED dayendId={}", dto.getDayendId());
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            log.error("approveDayEndById failed: dayendId={}, error={}", dto.getDayendId(), e.getMessage(), e);
            throw e;
        }
    }

    // ❌ REJECT BY ID — allowed from PENDING or SK_APPROVED
    public void rejectDayEndById(DayEndDto dto) {
        if (dto.getDayendId() == null)
            throw new RuntimeException("dayendId is required");
        if (dto.getRejectReason() == null || dto.getRejectReason().isBlank())
            throw new RuntimeException("Reject reason is required");

        log.info("rejectDayEndById: dayendId={}, reason={}", dto.getDayendId(), dto.getRejectReason());
        try {
            int updated = jdbcTemplate.update(
                "UPDATE dayend_approval " +
                "SET status = " + DayEndStatus.REJECTED + ", reject_reason = ?, approved_at = NULL " +
                "WHERE id = ? AND status IN (" + DayEndStatus.PENDING + "," + DayEndStatus.SK_APPROVED + ")",
                dto.getRejectReason(), dto.getDayendId());

            if (updated == 0) {
                log.warn("rejectDayEndById: no PENDING/SK_APPROVED record for dayendId={}", dto.getDayendId());
                throw new RuntimeException("No pending DayEnd found to reject");
            }
            approvalRepository.insert(dto.getDayendId(),
                    "STOREKEEPER".equalsIgnoreCase(dto.getApproverRole()) ? "STOREKEEPER" : "ACCOUNTS",
                    "REJECTED", currentUser(), null, dto.getRejectReason());
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
                  AND da.status IN (%d, %d, %d)
                ORDER BY da.start_time DESC
            """.formatted(DayEndStatus.STARTED, DayEndStatus.PENDING, DayEndStatus.SK_APPROVED);

            List<OnlineAgentDto> result = jdbcTemplate.query(sql, (rs, rowNum) -> {
                OnlineAgentDto dto = new OnlineAgentDto();
                dto.setDeliveryId(rs.getLong("delivery_id"));
                dto.setDeliveryBoyName(rs.getString("delivery_boy_name"));
                dto.setStatus(DayEndStatus.statusName(rs.getInt("status")));
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

    // 🔍 LIST
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
                    res.setStatus(DayEndStatus.statusName(rs.getInt("status")));
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