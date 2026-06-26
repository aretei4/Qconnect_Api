package com.api.distr.docs.company;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Company registry endpoints.
 *
 * GET  /api/company/search?q=ME  → search (public — called before login)
 * GET  /api/company              → list all active companies
 * POST /api/company              → add a new company
 * PUT  /api/company/{id}         → update name / code / baseUrl / active flag
 * DELETE /api/company/{id}       → deactivate (soft-delete)
 */
@RestController
@RequestMapping("/api/company")
public class CompanyController {

    private final CompanyService service;

    public CompanyController(CompanyService service) {
        this.service = service;
    }

    // ── Read ──────────────────────────────────────────────────────────────────

    @GetMapping("/search")
    public ResponseEntity<List<CompanyDto>> search(
            @RequestParam(defaultValue = "") String q) {
        return ResponseEntity.ok(service.search(q));
    }

    @GetMapping
    public ResponseEntity<List<CompanyDto>> listAll() {
        return ResponseEntity.ok(service.findAll());
    }

    // ── Create ────────────────────────────────────────────────────────────────

    @PostMapping
    public ResponseEntity<?> create(@RequestBody CompanyDto dto) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(service.create(dto));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // ── Update ────────────────────────────────────────────────────────────────

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody CompanyDto dto) {
        try {
            return ResponseEntity.ok(service.update(id, dto));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // ── Deactivate (soft-delete) ───────────────────────────────────────────────

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deactivate(@PathVariable Long id) {
        try {
            service.deactivate(id);
            return ResponseEntity.ok(Map.of("message", "Company deactivated"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
