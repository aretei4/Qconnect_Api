package com.api.distr.docs.sales;


import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.api.distr.docs.sales.dto.SalesEntry;
import com.api.distr.docs.sales.repo.SalesEntryRepository;

@RestController
@RequestMapping("/api/sales")
public class SalesEntryController {

    private final SalesEntryRepository repository;

    public SalesEntryController(SalesEntryRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public List<SalesEntry> getByFilters(@RequestParam Map<String, String> filters) {
        if (filters.isEmpty()) {
            throw new IllegalArgumentException("At least one filter must be provided.");
        }
        return repository.findByFilters(filters);
    }

    /** POST /api/sales/delete  —  body: ["PICK001","PICK002",...] */
    @PostMapping("/delete")
    public ResponseEntity<?> deleteByPicklistNos(@RequestBody List<String> picklistNos) {
        if (picklistNos == null || picklistNos.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "No picklist numbers provided"));
        }
        int deleted = repository.deleteByPicklistNos(picklistNos);
        return ResponseEntity.ok(Map.of("deleted", deleted));
    }
}

