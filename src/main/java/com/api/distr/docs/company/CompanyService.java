package com.api.distr.docs.company;

import jakarta.annotation.PostConstruct;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CompanyService {

    private final JdbcTemplate      jdbc;
    private final CompanyRepository repository;

    public CompanyService(JdbcTemplate jdbc, CompanyRepository repository) {
        this.jdbc       = jdbc;
        this.repository = repository;
    }

    // ── Table init ────────────────────────────────────────────────────────────

    @PostConstruct
    public void initTable() {
        jdbc.execute("""
                CREATE TABLE IF NOT EXISTS companies (
                    id       BIGSERIAL    PRIMARY KEY,
                    name     VARCHAR(100) NOT NULL,
                    code     VARCHAR(20)  NOT NULL UNIQUE,
                    base_url VARCHAR(255) NOT NULL,
                    active   BOOLEAN      NOT NULL DEFAULT true
                )
                """);
        // Companies are managed manually via API or direct DB inserts — no seed data here.
    }

    // ── Queries ───────────────────────────────────────────────────────────────

    public List<CompanyDto> search(String query) {
        if (query == null || query.isBlank()) return repository.findAll();
        return repository.search(query.trim());
    }

    public List<CompanyDto> findAll() {
        return repository.findAll();
    }

    // ── Create ────────────────────────────────────────────────────────────────

    public CompanyDto create(CompanyDto dto) {
        validate(dto);
        return repository.insert(dto);
    }

    // ── Update ────────────────────────────────────────────────────────────────

    public CompanyDto update(Long id, CompanyDto dto) {
        if (id == null) throw new IllegalArgumentException("id is required");
        validate(dto);
        int rows = repository.update(id, dto);
        if (rows == 0) throw new IllegalArgumentException("Company not found: " + id);
        return repository.findById(id);
    }

    // ── Deactivate ────────────────────────────────────────────────────────────

    public void deactivate(Long id) {
        if (id == null) throw new IllegalArgumentException("id is required");
        int rows = repository.deactivate(id);
        if (rows == 0) throw new IllegalArgumentException("Company not found: " + id);
    }

    // ── Validation ────────────────────────────────────────────────────────────

    private void validate(CompanyDto dto) {
        if (dto.getName()    == null || dto.getName().isBlank())
            throw new IllegalArgumentException("name is required");
        if (dto.getCode()    == null || dto.getCode().isBlank())
            throw new IllegalArgumentException("code is required");
        if (dto.getBaseUrl() == null || dto.getBaseUrl().isBlank())
            throw new IllegalArgumentException("baseUrl is required");
    }
}
