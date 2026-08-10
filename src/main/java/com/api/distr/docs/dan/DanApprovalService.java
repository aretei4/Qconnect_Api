package com.api.distr.docs.dan;

import com.api.distr.docs.dan.dto.DanApprovalDto;
import com.api.distr.docs.dan.dto.DanApprovalRequest;
import com.api.distr.docs.dan.dto.DanApprovalStatusDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * Two-desk DAN approval workflow:
 *
 *   submitted → STOREKEEPER approves → ACCOUNTS approves → DAN APPROVED
 *
 * Every action is appended to dan_approval_log with the acting user, so the
 * trail answers "who approved this, when, and what did they say".
 * A rejection at any stage sets the DAN to REJECTED.
 */
@Service
public class DanApprovalService {

    private static final Logger log = LoggerFactory.getLogger(DanApprovalService.class);
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    public static final String STAGE_STOREKEEPER = "STOREKEEPER";
    public static final String STAGE_ACCOUNTS    = "ACCOUNTS";
    private static final List<String> STAGES = List.of(STAGE_STOREKEEPER, STAGE_ACCOUNTS);

    private final DanApprovalRepository repo;

    public DanApprovalService(DanApprovalRepository repo) { this.repo = repo; }

    // ── Record an approval / rejection ────────────────────────────────────────

    @Transactional
    public DanApprovalStatusDto record(Long danId, DanApprovalRequest req) {
        if (danId == null)
            throw new IllegalArgumentException("danId is required");
        if (!repo.danExists(danId))
            throw new IllegalArgumentException("DAN not found: " + danId);

        String stage = req.getStage() != null ? req.getStage().trim().toUpperCase() : "";
        if (!STAGES.contains(stage))
            throw new IllegalArgumentException("stage must be one of " + STAGES + ", got: " + req.getStage());

        String action = req.getAction() != null && !req.getAction().isBlank()
                ? req.getAction().trim().toUpperCase() : "APPROVED";
        if (!List.of("APPROVED", "REJECTED").contains(action))
            throw new IllegalArgumentException("action must be APPROVED or REJECTED, got: " + req.getAction());
        if ("REJECTED".equals(action) && (req.getRemarks() == null || req.getRemarks().isBlank()))
            throw new IllegalArgumentException("remarks are required when rejecting");

        // Enforce order: accounts cannot sign off before the storekeeper has
        if (STAGE_ACCOUNTS.equals(stage) && "APPROVED".equals(action)
                && !repo.getLatestApprovals(danId).containsKey(STAGE_STOREKEEPER)) {
            throw new IllegalStateException("Storekeeper approval is required before accounts can approve");
        }

        String user = req.getApprovedBy() != null && !req.getApprovedBy().isBlank()
                ? req.getApprovedBy() : currentUsername();
        String role = req.getApprovedByRole() != null && !req.getApprovedByRole().isBlank()
                ? req.getApprovedByRole() : currentRole();

        repo.insert(danId, stage, action, user, role, req.getRemarks());

        if ("REJECTED".equals(action)) {
            repo.markRejected(danId, req.getRemarks());
        } else {
            // Both desks signed off → the DAN is fully approved
            Map<String, Map<String, Object>> approvals = repo.getLatestApprovals(danId);
            if (approvals.containsKey(STAGE_STOREKEEPER) && approvals.containsKey(STAGE_ACCOUNTS)) {
                repo.markApproved(danId);
                log.info("DAN {} fully approved (storekeeper + accounts)", danId);
            }
        }

        return getStatus(danId);
    }

    // ── Read ──────────────────────────────────────────────────────────────────

    public DanApprovalStatusDto getStatus(Long danId) {
        if (!repo.danExists(danId))
            throw new IllegalArgumentException("DAN not found: " + danId);

        Map<String, Map<String, Object>> approvals = repo.getLatestApprovals(danId);

        DanApprovalStatusDto dto = new DanApprovalStatusDto();
        dto.setDanId(danId);
        dto.setDanStatus(repo.getDanStatus(danId));
        dto.setTrail(repo.getTrail(danId));

        applyStage(dto, approvals.get(STAGE_STOREKEEPER), true);
        applyStage(dto, approvals.get(STAGE_ACCOUNTS),    false);

        dto.setFullyApproved(dto.isStorekeeperApproved() && dto.isAccountsApproved());
        dto.setPendingStage(!dto.isStorekeeperApproved() ? STAGE_STOREKEEPER
                          : !dto.isAccountsApproved()    ? STAGE_ACCOUNTS
                          : null);
        return dto;
    }

    public List<DanApprovalDto> getTrail(Long danId) {
        return repo.getTrail(danId);
    }

    public List<Map<String, Object>> getPending(String stage) {
        String s = stage != null ? stage.trim().toUpperCase() : STAGE_STOREKEEPER;
        if (!STAGES.contains(s))
            throw new IllegalArgumentException("stage must be one of " + STAGES);
        return repo.getPendingForStage(s);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void applyStage(DanApprovalStatusDto dto, Map<String, Object> entry, boolean storekeeper) {
        if (entry == null) return;
        String by = String.valueOf(entry.get("by"));
        Object ts = entry.get("at");
        String at = ts instanceof Timestamp t ? t.toLocalDateTime().format(FMT) : null;
        if (storekeeper) {
            dto.setStorekeeperApproved(true);
            dto.setStorekeeperBy(by);
            dto.setStorekeeperAt(at);
        } else {
            dto.setAccountsApproved(true);
            dto.setAccountsBy(by);
            dto.setAccountsAt(at);
        }
    }

    /** True for Spring's anonymous placeholder authentication (no real JWT user). */
    private static boolean isAnonymous(org.springframework.security.core.Authentication auth) {
        return auth == null
                || auth instanceof org.springframework.security.authentication.AnonymousAuthenticationToken
                || "anonymousUser".equals(auth.getName());
    }

    /** Username from the JWT-populated security context, or "system" when unauthenticated. */
    private String currentUsername() {
        try {
            var auth = SecurityContextHolder.getContext().getAuthentication();
            if (!isAnonymous(auth) && auth.getName() != null && !auth.getName().isBlank())
                return auth.getName();
        } catch (Exception ignored) { }
        return "system";
    }

    /** Role from the JWT authorities, or null when unauthenticated. */
    private String currentRole() {
        try {
            var auth = SecurityContextHolder.getContext().getAuthentication();
            if (!isAnonymous(auth) && auth.getAuthorities() != null) {
                return auth.getAuthorities().stream()
                        .map(a -> a.getAuthority().replaceFirst("^ROLE_", ""))
                        .filter(r -> !"ANONYMOUS".equals(r))
                        .findFirst()
                        .orElse(null);
            }
        } catch (Exception ignored) { }
        return null;
    }
}
