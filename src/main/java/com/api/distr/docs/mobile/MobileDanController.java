package com.api.distr.docs.mobile;

import com.api.distr.docs.mobile.dto.MobileAgentDto;
import com.api.distr.docs.mobile.dto.MobileApproveRequest;
import com.api.distr.docs.mobile.dto.MobileInvoiceDto;
import com.api.distr.docs.mobile.dto.MobilePaymentSummaryDto;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * /api/mobile/dan/* — three-step mobile DAN-close flow.
 *
 * GET  /open-list                 → agents with an open DAN            (step 1)
 * GET  /{danId}/invoices          → store cards + return rows          (step 2)
 * GET  /{danId}/payment-summary   → pre-filled mode amounts + totals   (step 3)
 * POST /{danId}/approve           → submit returns + payments
 */
@RestController
@RequestMapping("/api/mobile/dan")
public class MobileDanController {

    private final MobileDanService service;
    private final UrlCryptoService urlCrypto;

    /** Frontend origin the encrypted link points at. */
    @org.springframework.beans.factory.annotation.Value("${app.mobile.base-url:http://localhost:5173}")
    private String mobileBaseUrl;

    /** Encrypted-link lifetime in minutes. */
    @org.springframework.beans.factory.annotation.Value("${app.mobile.link-expiry-minutes:1440}")
    private long linkExpiryMinutes;

    public MobileDanController(MobileDanService service, UrlCryptoService urlCrypto) {
        this.service   = service;
        this.urlCrypto = urlCrypto;
    }

    // ── Encrypted URL ─────────────────────────────────────────────────────────

    /**
     * GET /api/mobile/dan/secure-url?danId=45&agentId=1
     * Returns an encrypted deep link for the mobile web view:
     *   { url, token, expiresAt }
     * The token payload is "danId|agentId|expiryEpochMillis", AES-256-GCM encrypted.
     */
    @GetMapping("/secure-url")
    public ResponseEntity<Map<String, Object>> secureUrl(
            @RequestParam Long danId,
            @RequestParam String agentId) {
        long expiresAt = System.currentTimeMillis() + linkExpiryMinutes * 60_000L;
        String token   = urlCrypto.encrypt(danId + "|" + agentId + "|" + expiresAt);
        String url     = mobileBaseUrl + "/m/dan-close?t=" + token;
        return ResponseEntity.ok(Map.of(
                "url",       url,
                "token",     token,
                "expiresAt", expiresAt));
    }

    /**
     * GET /api/mobile/dan/resolve?token=…
     * Decrypts a secure-url token → { valid, danId, agentId, expiresAt }.
     * valid=false when tampered or expired.
     */
    @GetMapping("/resolve")
    public ResponseEntity<Map<String, Object>> resolve(@RequestParam String token) {
        try {
            String[] parts = urlCrypto.decrypt(token).split("\\|");
            long expiresAt = Long.parseLong(parts[2]);
            boolean valid  = System.currentTimeMillis() <= expiresAt;
            return ResponseEntity.ok(Map.of(
                    "valid",     valid,
                    "danId",     Long.parseLong(parts[0]),
                    "agentId",   parts[1],
                    "expiresAt", expiresAt));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of("valid", false));
        }
    }

    @GetMapping("/open-list")
    public ResponseEntity<List<MobileAgentDto>> openList() {
        return ResponseEntity.ok(service.getOpenDans());
    }

    @GetMapping("/{danId}/invoices")
    public ResponseEntity<List<MobileInvoiceDto>> invoices(@PathVariable Long danId) {
        return ResponseEntity.ok(service.getInvoices(danId));
    }

    @GetMapping("/{danId}/payment-summary")
    public ResponseEntity<MobilePaymentSummaryDto> paymentSummary(@PathVariable Long danId) {
        return ResponseEntity.ok(service.getPaymentSummary(danId));
    }

    /** Per-invoice payment breakdown for step 3 (invoice, customer, mode details). */
    @GetMapping("/{danId}/payment-detail")
    public ResponseEntity<?> paymentDetail(@PathVariable Long danId) {
        try {
            return ResponseEntity.ok(service.getPaymentDetail(danId));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName()));
        }
    }

    /**
     * POST /{danId}/storekeeper-approve — step 2 "Approve & Next".
     * Saves the settled returns and records the STOREKEEPER sign-off.
     */
    @PostMapping("/{danId}/storekeeper-approve")
    public ResponseEntity<?> storekeeperApprove(
            @PathVariable Long danId,
            @RequestBody MobileApproveRequest req) {
        try {
            return ResponseEntity.ok(service.approveStorekeeper(danId, req));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("success", false, "message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message",
                            e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName()));
        }
    }

    /**
     * POST /{danId}/accounts-approve — step 3 "Approve".
     * Saves payments, closes the DAN and records the ACCOUNTS sign-off.
     */
    @PostMapping("/{danId}/accounts-approve")
    public ResponseEntity<?> accountsApprove(
            @PathVariable Long danId,
            @RequestBody MobileApproveRequest req) {
        try {
            return ResponseEntity.ok(service.approveAccounts(danId, req));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("success", false, "message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message",
                            e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName()));
        }
    }
}
