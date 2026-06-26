package com.api.distr.docs.config;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * Feature flag CRUD service.
 * Schema and seed data: src/main/resources/db/V9__create_feature_config.sql
 */
@Service
public class FeatureConfigService {

    private static final Logger log = LoggerFactory.getLogger(FeatureConfigService.class);

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // ── READ ──────────────────────────────────────────────────────────────────

    public List<FeatureConfigDTO> getAll() {
        log.info("getAll: fetching feature configs");
        try {
            List<FeatureConfigDTO> result = jdbcTemplate.query("""
                    SELECT feature_key, enabled, label, description, category, updated_at
                    FROM feature_config
                    ORDER BY category, label
                    """, this::mapRow);
            log.info("getAll: returned {} feature configs", result.size());
            return result;
        } catch (Exception e) {
            log.error("getAll failed: error={}", e.getMessage(), e);
            throw e;
        }
    }

    public FeatureConfigDTO getByKey(String key) {
        log.info("getByKey: key={}", key);
        try {
            return jdbcTemplate.queryForObject("""
                    SELECT feature_key, enabled, label, description, category, updated_at
                    FROM feature_config WHERE feature_key = ?
                    """, this::mapRow, key);
        } catch (Exception e) {
            log.error("getByKey failed: key={}, error={}", key, e.getMessage(), e);
            throw e;
        }
    }

    /** Quick boolean check used by other services to gate behaviour. */
    public boolean isEnabled(String key) {
        try {
            Boolean val = jdbcTemplate.queryForObject(
                    "SELECT enabled FROM feature_config WHERE feature_key = ?", Boolean.class, key);
            return Boolean.TRUE.equals(val);
        } catch (Exception e) {
            log.warn("isEnabled: key={} not found, defaulting to true", key);
            return true; // safe default — if config is missing, don't break the feature
        }
    }

    // ── UPDATE ────────────────────────────────────────────────────────────────

    public FeatureConfigDTO toggle(String key, boolean enabled) {
        log.info("toggle: key={}, enabled={}", key, enabled);
        try {
            int rows = jdbcTemplate.update("""
                    UPDATE feature_config
                    SET enabled = ?, updated_at = NOW()
                    WHERE feature_key = ?
                    """, enabled, key);

            if (rows == 0) {
                log.warn("toggle: key={} not found", key);
                throw new RuntimeException("Feature not found: " + key);
            }
            log.info("toggle: key={} set to {}", key, enabled);
            return getByKey(key);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            log.error("toggle failed: key={}, error={}", key, e.getMessage(), e);
            throw e;
        }
    }

    /** Bulk update — replaces all enabled flags in one call from the settings page. */
    public void saveAll(List<FeatureConfigDTO> features) {
        log.info("saveAll: updating {} features", features.size());
        try {
            for (FeatureConfigDTO f : features) {
                jdbcTemplate.update("""
                        UPDATE feature_config
                        SET enabled = ?, updated_at = NOW()
                        WHERE feature_key = ?
                        """, f.isEnabled(), f.getKey());
            }
            log.info("saveAll: completed {} updates", features.size());
        } catch (Exception e) {
            log.error("saveAll failed: error={}", e.getMessage(), e);
            throw e;
        }
    }

    // ── Row mapper ─────────────────────────────────────────────────────────────

    private FeatureConfigDTO mapRow(java.sql.ResultSet rs, int rowNum) throws java.sql.SQLException {
        FeatureConfigDTO d = new FeatureConfigDTO();
        d.setKey(rs.getString("feature_key"));
        d.setEnabled(rs.getBoolean("enabled"));
        d.setLabel(rs.getString("label"));
        d.setDescription(rs.getString("description"));
        d.setCategory(rs.getString("category"));
        java.sql.Timestamp ts = rs.getTimestamp("updated_at");
        if (ts != null) d.setUpdatedAt(ts.toLocalDateTime());
        return d;
    }
}
