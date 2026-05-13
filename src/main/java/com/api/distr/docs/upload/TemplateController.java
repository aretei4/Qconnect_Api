package com.api.distr.docs.upload;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/template")
public class TemplateController {

    @Autowired
    private TemplateService templateService;

    @PostMapping("/save")
    public ResponseEntity<?> saveTemplate(@RequestBody ExcelTemplate template) {
        templateService.saveTemplate(template);
       
        return ResponseEntity.ok( Map.of("message", "Template saved successfully"));
    }

    // ── Literal paths first (must come before the /{templateName} wildcard) ──

    @GetMapping("/names")
    public ResponseEntity<List<String>> getTemplateNames() {
        return ResponseEntity.ok(templateService.getAllTemplateNames());
    }

    /** GET /api/template/companies — all distinct company names across all templates */
    @GetMapping("/companies")
    public ResponseEntity<List<String>> getCompanies() {
        return ResponseEntity.ok(templateService.getDistinctCompanies());
    }

    /** GET /api/template/sales-companies — returns all invoice/sales templates with their company names */
    @GetMapping("/sales-companies")
    public ResponseEntity<List<ExcelTemplate>> getSalesCompanies() {
        return ResponseEntity.ok(templateService.getSalesCompanies());
    }

    /** GET /api/template/by-company/{companyName} — all templates for a specific company */
    @GetMapping("/by-company/{companyName}")
    public ResponseEntity<List<ExcelTemplate>> getByCompany(@PathVariable String companyName) {
        return ResponseEntity.ok(templateService.getTemplatesByCompany(companyName));
    }

    // ── Wildcard — must be last ───────────────────────────────────────────────

    @GetMapping("/{templateName}")
    public ResponseEntity<?> getTemplate(@PathVariable String templateName) {
        ExcelTemplate template = templateService.getTemplate(templateName);
        return ResponseEntity.ok(template);
    }
}

