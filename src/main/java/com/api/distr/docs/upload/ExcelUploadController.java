package com.api.distr.docs.upload;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;

@RestController
@RequestMapping("/api/excel")
public class ExcelUploadController {

    private final SalesExcelService service;

    public ExcelUploadController(SalesExcelService service) {
        this.service = service;
    } 
    @Autowired
  DeliveryExcelService deliveryService;

    @Autowired
    private ExcelUploadService tempservice;

    @Autowired
    private TemplateService templateservice;
    
    private static final long   MAX_FILE_SIZE_BYTES = 10 * 1024 * 1024; // 10 MB
    private static final List<String> ALLOWED_EXTENSIONS = List.of(".xlsx", ".xls");

    /** Validates the uploaded file — returns error message or null if OK. */
    private String validateFile(MultipartFile file) {
        if (file == null || file.isEmpty())
            return "No file provided or file is empty.";

        String filename = file.getOriginalFilename();
        if (filename == null || filename.isBlank())
            return "File name is missing.";

        String lower = filename.toLowerCase();
        boolean validExt = ALLOWED_EXTENSIONS.stream().anyMatch(lower::endsWith);
        if (!validExt)
            return "Invalid file type '" + filename.substring(filename.lastIndexOf('.') + 1)
                    + "'. Only .xlsx and .xls files are allowed.";

        if (file.getSize() > MAX_FILE_SIZE_BYTES)
            return String.format("File size %.1f MB exceeds the 10 MB limit.",
                    file.getSize() / (1024.0 * 1024));

        String contentType = file.getContentType();
        if (contentType != null &&
            !contentType.contains("spreadsheet") &&
            !contentType.contains("excel") &&
            !contentType.equals("application/octet-stream")) {
            return "Invalid content type: " + contentType + ". Please upload a valid Excel file.";
        }

        return null; // OK
    }

    @PostMapping("/upload")
    public ResponseEntity<?> uploadFile(@RequestParam("file") MultipartFile file,
                                        @RequestParam("type") String type) {
        // ── File validation ────────────────────────────────────────────────────
        String fileError = validateFile(file);
        if (fileError != null)
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", fileError));

        if (type == null || type.isBlank())
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", "Upload type is required."));

        try {
            ExcelTemplate excelTemp  = templateservice.getTemplate(type);
            Map<String, String> mappings = excelTemp.getMappings();
            String temTyple              = excelTemp.getTemplateType();
            String companyName           = excelTemp.getCompanyName();

            if (mappings == null || mappings.isEmpty())
                return ResponseEntity.badRequest().body(
                        Map.of("success", false, "error", "No column mappings configured for template type: " + type));

            List<String> errors = new ArrayList<>();
            if (temTyple.equalsIgnoreCase("customer")) {
                tempservice.excelMaster(file, mappings, "C");
            } else if (temTyple.equalsIgnoreCase("delivery")) {
                tempservice.excelMaster(file, mappings, "D");
            } else {
                errors = tempservice.importExcel(file, mappings, companyName);
            }

            if (!errors.isEmpty()) {
                return ResponseEntity.ok(Map.of(
                        "success", false,
                        "errors",  errors,
                        "message", "Upload completed with " + errors.size() + " error(s)"));
            }
            return ResponseEntity.ok(Map.of("success", true, "errors", List.of(), "message", "Excel uploaded successfully"));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("success", false, "error", "Upload failed: " + e.getMessage()));
        }
    }
    
    @PostMapping("/uploadTemplate")
    public ResponseEntity<?> uploadExcel(
            @RequestParam("file") MultipartFile file,
            @RequestParam("type") String type) {

        System.out.println("File type: " + type);

        List<String> errors = new ArrayList<>();

        switch (type.toLowerCase()) {

            case "sales":
                errors = service.processSalesExcel(file);
                break;

            case "agent":
                errors = deliveryService.uploadExcel(file);
                break;

            case "delivery":
                errors = service.processSalesExcel(file);
                break;

            default:
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "errors", List.of("Invalid type. Allowed values: sales, customer, delivery")
                ));
        }

        return ResponseEntity.ok(Map.of(
                "success", errors.isEmpty(),
                "errors", errors,
                "message", errors.isEmpty()
                        ? "Upload Successful"
                        : "Upload Completed with Some Errors"
        ));
    }

    @GetMapping("/customerList")
    public ResponseEntity<?> getAllCustomers() {
        return ResponseEntity.ok(service.getAllCustomers());
    }
    
    @PostMapping("/saveCustomer")
    public ResponseEntity<?> saveCustomer(@RequestBody CustomerDTO dto) {
        try {
            service.saveOrUpdateCustomer(dto);
            return ResponseEntity.ok(Map.of("message", "Customer saved successfully"));
        } catch (Exception e) {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }
}

