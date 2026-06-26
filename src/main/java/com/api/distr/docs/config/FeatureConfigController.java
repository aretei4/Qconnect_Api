package com.api.distr.docs.config;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST endpoints for feature flag management.
 *
 * GET  /api/config/features              — all feature flags
 * GET  /api/config/features/{key}        — single feature flag
 * PUT  /api/config/features/{key}/toggle — toggle enabled/disabled
 * PUT  /api/config/features/save-all     — bulk update from settings page
 */
@RestController
@RequestMapping("/api/config")
public class FeatureConfigController {

    @Autowired
    private FeatureConfigService featureConfigService;

    @GetMapping("/features")
    public ResponseEntity<List<FeatureConfigDTO>> getAll() {
        try {
            return ResponseEntity.ok(featureConfigService.getAll());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/features/{key}")
    public ResponseEntity<?> getByKey(@PathVariable String key) {
        try {
            return ResponseEntity.ok(featureConfigService.getByKey(key));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Feature not found: " + key);
        }
    }

    @PutMapping("/features/{key}/toggle")
    public ResponseEntity<?> toggle(
            @PathVariable String key,
            @RequestBody Map<String, Boolean> body) {
        Boolean enabled = body.get("enabled");
        if (enabled == null)
            return ResponseEntity.badRequest().body("'enabled' field is required");
        try {
            FeatureConfigDTO updated = featureConfigService.toggle(key, enabled);
            return ResponseEntity.ok(updated);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to toggle feature: " + e.getMessage());
        }
    }

    @PutMapping("/features/save-all")
    public ResponseEntity<?> saveAll(@RequestBody List<FeatureConfigDTO> features) {
        try {
            featureConfigService.saveAll(features);
            return ResponseEntity.ok(Map.of("success", true, "message", "Settings saved"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", "Failed to save: " + e.getMessage()));
        }
    }
}
