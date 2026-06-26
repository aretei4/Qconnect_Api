package com.api.distr.docs.company;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;

@Repository
public class CompanyRepository {

    private final JdbcTemplate jdbc;

    public CompanyRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    // ── Row mapper ────────────────────────────────────────────────────────────

    private static CompanyDto mapRow(java.sql.ResultSet rs, int rowNum) throws java.sql.SQLException {
        return new CompanyDto(
                rs.getLong("id"),
                rs.getString("name"),
                rs.getString("code"),
                rs.getString("base_url"));
    }

    // ── Read ──────────────────────────────────────────────────────────────────

    public List<CompanyDto> search(String query) {
        String like = "%" + query.toUpperCase() + "%";
        return jdbc.query("""
                SELECT id, name, code, base_url
                FROM   companies
                WHERE  active = true
                  AND  (UPPER(name) LIKE ? OR UPPER(code) LIKE ?)
                ORDER  BY name
                LIMIT  10
                """, CompanyRepository::mapRow, like, like);
    }

    public List<CompanyDto> findAll() {
        return jdbc.query("""
                SELECT id, name, code, base_url
                FROM   companies
                WHERE  active = true
                ORDER  BY name
                """, CompanyRepository::mapRow);
    }

    public CompanyDto findById(Long id) {
        return jdbc.queryForObject("""
                SELECT id, name, code, base_url FROM companies WHERE id = ?
                """, CompanyRepository::mapRow, id);
    }

    // ── Write ─────────────────────────────────────────────────────────────────

    public CompanyDto insert(CompanyDto dto) {
        KeyHolder keys = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO companies (name, code, base_url) VALUES (?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, dto.getName());
            ps.setString(2, dto.getCode());
            ps.setString(3, dto.getBaseUrl());
            return ps;
        }, keys);

        long id = keys.getKey().longValue();
        return findById(id);
    }

    public int update(Long id, CompanyDto dto) {
        return jdbc.update("""
                UPDATE companies
                SET name     = ?,
                    code     = ?,
                    base_url = ?,
                    active   = ?
                WHERE id = ?
                """,
                dto.getName(),
                dto.getCode(),
                dto.getBaseUrl(),
                dto.isActive(),
                id);
    }

    public int deactivate(Long id) {
        return jdbc.update("UPDATE companies SET active = false WHERE id = ?", id);
    }
}
