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
    
    @PostMapping("/upload")
    public ResponseEntity<?> uploadFile(@RequestParam("file") MultipartFile file,
    		   @RequestParam("type") String type) {
    	  try {
    		  List<String> errors = new ArrayList<>();
    	ExcelTemplate excelTemp = templateservice.getTemplate(type);
        // Normally fetch mapping from DB;
        Map<String, String> mappings =excelTemp.getMappings();          
        	tempservice.importExcel(file, mappings);
        	
        	return ResponseEntity.ok(
        		    Map.of("message", "Excel uploaded successfully")
        		);

            //return ResponseEntity.ok("Excel uploaded successfully!");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
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

