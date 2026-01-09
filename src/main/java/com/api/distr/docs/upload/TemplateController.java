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

    @GetMapping("/{templateName}")
    public ResponseEntity<?> getTemplate(@PathVariable String templateName) {
        ExcelTemplate template = templateService.getTemplate(templateName);
        return ResponseEntity.ok(template);
    }
    
    @GetMapping("/names")
    public ResponseEntity<List<String>> getTemplateNames() {
        return ResponseEntity.ok(templateService.getAllTemplateNames());
    }
}

