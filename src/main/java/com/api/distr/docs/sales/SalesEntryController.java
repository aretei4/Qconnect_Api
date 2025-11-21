package com.api.distr.docs.sales;


import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
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

    /**
     * Example: /api/sales?Customer_no=CUST1001&Billing_Date=2025-10-28
     */
    @GetMapping
    public List<SalesEntry> getByFilters(@RequestParam Map<String, String> filters) {
        if (filters.isEmpty()) {
            throw new IllegalArgumentException("At least one filter must be provided.");
        }
        return repository.findByFilters(filters);
    }
    
}

