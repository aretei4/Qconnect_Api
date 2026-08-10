package com.api.distr.docs.user;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * One-time session handoff between the Android app and the React web app
 * rendered inside its WebView.
 *
 * Flow:
 *   1. Android POSTs the logged-in user's details → gets a short-lived one-time code.
 *   2. Android loads  /mobile-dan-close?code={code}  in the WebView.
 *   3. The React page GETs /api/auth/web-session/{code} → receives the user details
 *      (token, userId, username, fullName, role) and stores them as its own session.
 *
 * Codes are single-use and expire after TTL_MS. Stored in-memory only — a lost code
 * simply means the WebView shows the login screen, and the app can request a new one.
 */
@RestController
@RequestMapping("/api/auth/web-session")
public class WebSessionController {

    private static final Logger log = LoggerFactory.getLogger(WebSessionController.class);

    private static final long TTL_MS = 2 * 60 * 1000;   // 2 minutes

    private static class Entry {
        final Map<String, Object> payload;
        final long expiresAt;
        Entry(Map<String, Object> payload) {
            this.payload   = payload;
            this.expiresAt = System.currentTimeMillis() + TTL_MS;
        }
        boolean expired() { return System.currentTimeMillis() > expiresAt; }
    }

    private final ConcurrentHashMap<String, Entry> store = new ConcurrentHashMap<>();

    /** Android → backend: register user details, receive a one-time code. */
    @PostMapping
    public ResponseEntity<Map<String, String>> create(@RequestBody Map<String, Object> userDetails) {
        if (userDetails == null || userDetails.get("token") == null
                || userDetails.get("token").toString().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "token is required"));
        }

        // opportunistic cleanup of expired codes
        store.entrySet().removeIf(e -> e.getValue().expired());

        String code = UUID.randomUUID().toString().replace("-", "");
        store.put(code, new Entry(Map.copyOf(userDetails)));
        log.info("web-session: created code for user={}", userDetails.get("username"));
        return ResponseEntity.ok(Map.of("code", code));
    }

    /** React page → backend: exchange the code (single use) for the user details. */
    @GetMapping("/{code}")
    public ResponseEntity<Map<String, Object>> consume(@PathVariable String code) {
        Entry entry = store.remove(code);
        if (entry == null || entry.expired()) {
            log.warn("web-session: code invalid or expired");
            return ResponseEntity.status(410).body(Map.of("error", "Code invalid or expired"));
        }
        log.info("web-session: consumed code for user={}", entry.payload.get("username"));
        return ResponseEntity.ok(entry.payload);
    }
}
