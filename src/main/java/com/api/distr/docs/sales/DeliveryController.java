package com.api.distr.docs.sales;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.api.distr.docs.sales.dto.ApiResponse;
import com.api.distr.docs.sales.dto.DeliveryAgent;
import com.api.distr.docs.sales.dto.DeliveryRequest;
import com.api.distr.docs.sales.dto.DeliveryStatus;
import com.api.distr.docs.sales.dto.DeliveryStatusDTO;
import com.api.distr.docs.sales.dto.OtpRequest;
import com.api.distr.docs.sales.dto.OtpResponse;
import com.api.distr.docs.sales.dto.SalesEntry;
import com.api.distr.docs.sales.repo.DeliveryStatusService;

@RestController
@RequestMapping("/api/delivery")
public class DeliveryController {

    private final DeliveryService deliveryService;
    public DeliveryController(DeliveryService deliveryService) {
        this.deliveryService = deliveryService;
    }
  
    @Autowired
    private DeliveryStatusService service;

    @GetMapping("/statusList")
    public List<DeliveryStatusDTO> filter(
            @RequestParam String fromDate,
            @RequestParam String toDate
    ) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

        LocalDate from = LocalDate.parse(fromDate, formatter);
        LocalDate to = LocalDate.parse(toDate, formatter);

        return service.getByUpdateDate(from, to);
    }
    
    
    @PostMapping("/delivery-status")
    public ResponseEntity<?> upsertDelivery(@RequestBody DeliveryStatus deliveryStatus) {
    	String msg="";
        try {
        	msg=deliveryService.upsertByPicklistNo(deliveryStatus);
            return ResponseEntity.ok().body(
                    new ApiResponse(true, msg)
            );
        } catch (RuntimeException ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse(false, ex.getMessage()));
        }
    }
    
    @PostMapping("/assign")
    public String assignDelivery(@RequestBody DeliveryRequest request) {
        return deliveryService.assignDeliveries(request);
    }
    @GetMapping("/allAgents")
    public List<DeliveryAgent> getAllAgents() {
        return deliveryService.getAllAgents();
    }
    
    @GetMapping("/deliveryList")
    public List<SalesEntry> getPicklists(@RequestParam Map<String, String> filters) {
        if (filters.isEmpty()) {
            throw new IllegalArgumentException("At least one filter must be provided.");
        }
        return deliveryService.getDeliveryList(filters);
    }
    
    @PostMapping("/send-otp")
    public ResponseEntity<OtpResponse> generateOtp(@RequestBody OtpRequest request) {
        try {
            // Example: you can later check from DB if picklistNo/mobile exist
            if (request.getMobile() == null || request.getPicklistNo() == null) {
                return ResponseEntity.badRequest().body(new OtpResponse("", false));
            }

            // Here you can generate or fetch OTP dynamically (currently hardcoded)
            String otp = "5678";

            return ResponseEntity.ok(new OtpResponse(otp, true));

        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(new OtpResponse("", false));
        }
    }
    @DeleteMapping("/delete/{picklistNo}")
    public ResponseEntity<?> deleteDelivery(@PathVariable String picklistNo) {
        String msg =""+deliveryService.deleteByPicklistNo(picklistNo);
       // new ApiResponse(true, msg)
        return ResponseEntity.ok().body(
                new ApiResponse(true, msg)
        );
      //  return ResponseEntity.ok(ApiResponse(true, msg));
    }
}

	

