package com.api.distr.docs.company;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CompanyService {

    private final CompanyRepository repository;

    public CompanyService(CompanyRepository repository) {
        this.repository = repository;
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
