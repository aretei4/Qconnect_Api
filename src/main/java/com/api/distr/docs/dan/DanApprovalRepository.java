package com.api.distr.docs.dan;

import com.api.distr.docs.dan.dto.DanApprovalDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.util.List;
import java.util.Map;

/**
 * Audit trail of who approved a DAN at each stage (dan_approval_log),
 * plus the roll-up stamps kept on dayend_approval for quick lookups.
 */
@Repository
public class DanApprovalRepository {

    private static final Logger log = LoggerFactory.getLogger(DanApprovalRepository.class);

    private final JdbcTemplate jdbc;

    public DanApprovalRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    // ── Write ─────────────────────────────────────────────────────────────────

    /** Appends one lifecycle event. stage and action are stored as integer codes. */
    public Long insert(Long danId, String stage, String action,
                       String approvedBy, String approvedByRole, String remarks) {
        int stageCode  = com.api.distr.docs.dayend.DayEndStatus.stageCode(stage);
        int actionCode = com.api.distr.docs.dayend.DayEndStatus.actionCode(action);
        Long id = jdbc.queryForObject("""
                INSERT INTO dan_approval_log
                    (dan_id, stage, action, approved_by, approved_by_role, remarks, created_at)
                VALUES (?, ?, ?, ?, ?, ?, NOW())
                RETURNING id
                """, Long.class, danId, stageCode, actionCode, approvedBy, approvedByRole, remarks);
        log.info("dan_approval_log: danId={}, stage={}, action={}, by={}", danId, stage, action, approvedBy);
        return id;
    }

    /**
     * Logs a lifecycle event (created / submitted / closed) without failing the
     * caller's transaction — the audit trail must never block the business flow.
     */
    public void logEvent(Long danId, String stage, String action, String by, String remarks) {
        if (danId == null) return;
        try {
            insert(danId, stage, action, by, null, remarks);
        } catch (Exception e) {
            log.warn("dan_approval_log: could not log {}/{} for danId={} — {}",
                    stage, action, danId, e.getMessage());
        }
    }

    /** Marks the DAN fully approved. */
    public void markApproved(Long danId) {
        jdbc.update(
                "UPDATE dayend_approval SET status = " + com.api.distr.docs.dayend.DayEndStatus.APPROVED +
                ", approved_at = NOW(), reject_reason = NULL WHERE id = ?",
                danId);
    }

    /** Marks the DAN rejected with a reason. */
    public void markRejected(Long danId, String reason) {
        jdbc.update(
                "UPDATE dayend_approval SET status = " + com.api.distr.docs.dayend.DayEndStatus.REJECTED +
                ", reject_reason = ?, approved_at = NULL WHERE id = ?",
                reason, danId);
    }

    // ── Read ──────────────────────────────────────────────────────────────────

    public boolean danExists(Long danId) {
        Integer n = jdbc.queryForObject(
                "SELECT COUNT(*) FROM dayend_approval WHERE id = ?", Integer.class, danId);
        return n != null && n > 0;
    }

    /** dayend_approval.status as its string name (e.g. "PENDING"), or null. */
    public String getDanStatus(Long danId) {
        try {
            Integer code = jdbc.queryForObject(
                    "SELECT status FROM dayend_approval WHERE id = ?", Integer.class, danId);
            return com.api.distr.docs.dayend.DayEndStatus.statusName(code);
        } catch (Exception e) {
            return null;
        }
    }

    /** Full trail for one DAN, oldest first. */
    public List<DanApprovalDto> getTrail(Long danId) {
        return jdbc.query("""
                SELECT id, dan_id, stage, action, approved_by, approved_by_role, remarks, created_at
                FROM dan_approval_log
                WHERE dan_id = ?
                ORDER BY created_at, id
                """, new Object[]{danId}, (rs, rn) -> {
            DanApprovalDto d = new DanApprovalDto();
            d.setId(rs.getLong("id"));
            d.setDanId(rs.getLong("dan_id"));
            d.setStage(com.api.distr.docs.dayend.DayEndStatus.stageName(rs.getInt("stage")));
            d.setAction(com.api.distr.docs.dayend.DayEndStatus.actionName(rs.getInt("action")));
            d.setApprovedBy(rs.getString("approved_by"));
            d.setApprovedByRole(rs.getString("approved_by_role"));
            d.setRemarks(rs.getString("remarks"));
            Timestamp ts = rs.getTimestamp("created_at");
            d.setCreatedAt(ts != null ? ts.toLocalDateTime() : null);
            return d;
        });
    }

    /** Latest APPROVED event per stage → { STOREKEEPER: {by, at}, ACCOUNTS: {by, at} }. */
    public Map<String, Map<String, Object>> getLatestApprovals(Long danId) {
        Map<String, Map<String, Object>> out = new java.util.HashMap<>();
        jdbc.query(
                "SELECT DISTINCT ON (stage) stage, approved_by, created_at " +
                "FROM dan_approval_log " +
                "WHERE dan_id = ? AND action = " + com.api.distr.docs.dayend.DayEndStatus.ACT_APPROVED + " " +
                "ORDER BY stage, created_at DESC",
                new Object[]{danId}, rs -> {
            String stage = com.api.distr.docs.dayend.DayEndStatus.stageName(rs.getInt("stage"));
            if (stage == null) return;
            out.put(stage, Map.of(
                    "by", rs.getString("approved_by") != null ? rs.getString("approved_by") : "",
                    "at", rs.getTimestamp("created_at")));
        });
        return out;
    }

    /** DANs whose given stage has no APPROVED entry yet (i.e. awaiting that desk). */
    public List<Map<String, Object>> getPendingForStage(String stage) {
        return jdbc.queryForList(
                "SELECT da.id AS dan_id, da.delivery_id, da.delivery_date, da.status, da.total_amount, " +
                "       COALESCE(dm.delivery_name, 'Agent ' || da.delivery_id) AS agent_name " +
                "FROM dayend_approval da " +
                "LEFT JOIN delivery_master dm ON dm.delivery_id = da.delivery_id " +
                "WHERE da.status IN (" + com.api.distr.docs.dayend.DayEndStatus.PENDING + "," +
                com.api.distr.docs.dayend.DayEndStatus.CLOSED + ") " +
                "  AND NOT EXISTS ( " +
                "      SELECT 1 FROM dan_approval_log l " +
                "      WHERE l.dan_id = da.id AND l.stage = ? AND l.action = " +
                com.api.distr.docs.dayend.DayEndStatus.ACT_APPROVED + ") " +
                "ORDER BY da.delivery_date DESC, da.id DESC",
                com.api.distr.docs.dayend.DayEndStatus.stageCode(stage));
    }
}
