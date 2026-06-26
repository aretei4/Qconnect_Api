package com.api.distr.docs.dan;

import com.api.distr.docs.dan.dto.DanListDto;
import com.api.distr.docs.dan.dto.DanPaymentDto;
import com.api.distr.docs.dan.dto.DanReturnDto;
import com.api.distr.docs.dan.dto.MobileReturnRequest;
import com.api.distr.docs.dan.dto.PamtReturnItem;
import com.api.distr.docs.dan.dto.WebReturnItemDto;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;


/**
 * /api/dan/*
 *
 * Return endpoints — all keyed by dir_id (unique per transaction) or by
 * deliveryId + picklistNo for listing. No danId required for returns.
 *
 * POST /api/dan/returns                              → save return, returns new dir_id
 * GET  /api/dan/returns/{dirId}                     → fetch items by transaction dir_id
 * GET  /api/dan/returns?deliveryId=X                → all returns for an agent
 * GET  /api/dan/returns?deliveryId=X&picklistNo=Y   → returns for a specific picklist
 *
 * POST /api/dan/{danId}/payment/{picklistNo}        → save payment
 * POST /api/dan/{danId}/submit                      → submit DAN for approval
 * GET  /api/dan/list                                → today's active agents
 */
@RestController
@RequestMapping("/api/dan")
public class DanController {

    private final DanService service;

    public DanController(DanService service) { this.service = service; }

    // ── DAN list ──────────────────────────────────────────────────────────────

    @GetMapping("/list")
    public ResponseEntity<List<DanListDto>> list() {
        try {
            return ResponseEntity.ok(service.getActiveDans());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ── Returns ───────────────────────────────────────────────────────────────

    /**
     * POST /api/dan/returns
     * Save a return submission. Generates a unique dir_id for this transaction.
     * Body: { deliveryId, picklistNo, ndType, reason, items[] }
     * Response: { success, dirId, saved, message }
     */
    @PostMapping("/returns")
    public ResponseEntity<Map<String, Object>> saveReturns(
            @RequestBody MobileReturnRequest request) {
        try {
            long dirId = service.saveMobileReturn(request);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "dirId",   dirId,
                    "saved",   request.getItems() != null ? request.getItems().size() : 0,
                    "message", "Return saved. Dir ID: " + dirId));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("success", false, "message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message",
                            e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName()));
        }
    }

    /**
     * GET /api/dan/returns/{dirId}
     * Fetch all items belonging to a specific return transaction.
     */
    @GetMapping("/returns/{dirId}")
    public ResponseEntity<?> getReturnsByDirId(@PathVariable long dirId) {
        try {
            return ResponseEntity.ok(service.getReturnsByDirId(dirId));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * GET /api/dan/returns?deliveryId=X
     * GET /api/dan/returns?deliveryId=X&picklistNo=Y
     * Fetch return items for an agent, optionally filtered by picklist.
     */
    @GetMapping("/returns")
    public ResponseEntity<?> getReturnsByDelivery(@RequestParam long deliveryId) {
        try {
            return ResponseEntity.ok(service.getReturnsByDelivery(deliveryId));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * GET /api/dan/returns/dire/{direId}
     * Fetch all return items for a given dire_id (unique transaction reference).
     * Used by web DanClosePage Step 2 to pre-populate return rows.
     */
    @GetMapping("/returns/dire/{direId}")
    public ResponseEntity<?> getReturnsByDireId(@PathVariable long direId) {
        try {
            return ResponseEntity.ok(service.getReturnsByDireId(direId));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * POST /api/dan/returns/dire/{direId}
     * Save return rows for a specific dire_id.
     * Body: array of { serial, description, billQty, billAmt, returnQty, returnAmt, reason, custom }
     * Used by web DanClosePage (per-picklist save).
     */
    @PostMapping("/returns/dire/{direId}")
    public ResponseEntity<Map<String, Object>> saveReturnsByDireId(
            @PathVariable long direId,
            @RequestBody List<WebReturnItemDto> items) {
        try {
            long dirId = service.saveReturnsByDireId(direId, items);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "dirId",   dirId,
                    "saved",   items != null ? items.size() : 0,
                    "message", "Returns saved for dire_id: " + direId));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("success", false, "message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message",
                            e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName()));
        }
    }

    // ── Pamt check ───────────────────────────────────────────────────────────

    /**
     * GET /api/dan/pamt-check?deliveryId=X
     * Returns all partial returns (nd_type='pamt') recorded today for the delivery agent.
     * These are returns where only an amount was entered — full item details are missing.
     * Used by the Day End screen to prompt the agent to complete them.
     */
    @GetMapping("/pamt-check")
    public ResponseEntity<?> getPamtReturns(@RequestParam long deliveryId) {
        try {
            List<PamtReturnItem> items = service.getPamtReturns(deliveryId);
            return ResponseEntity.ok(items);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // ── Payment ───────────────────────────────────────────────────────────────

    @PostMapping("/{danId}/payment/dire/{direId}")
    public ResponseEntity<Map<String, Object>> savePayment(
            @PathVariable Long danId,
            @PathVariable long direId,
            @RequestBody  DanPaymentDto dto) {
        try {
            service.savePayment(direId, dto);
            return ResponseEntity.ok(Map.of("success", true));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    // ── Submit ────────────────────────────────────────────────────────────────

    @PostMapping("/{danId}/submit")
    public ResponseEntity<Map<String, Object>> submit(@PathVariable Long danId) {
        try {
            service.submitDan(danId);
            return ResponseEntity.ok(Map.of("success", true, "message", "DAN submitted for approval"));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("success", false, "message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }
}
