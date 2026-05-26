package com.api.distr.docs.sales;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.api.distr.docs.sales.dto.ApiResponse;
import com.api.distr.docs.sales.dto.DeliveryAgent;
import com.api.distr.docs.sales.dto.DeliveryLoginRequest;
import com.api.distr.docs.sales.dto.DeliveryLoginResponse;
import com.api.distr.docs.sales.dto.DeliveryRequest;
import com.api.distr.docs.sales.dto.DeliveryStatus;
import com.api.distr.docs.sales.dto.DeliveryStatusDTO;
import com.api.distr.docs.sales.dto.OtpRequest;
import com.api.distr.docs.sales.dto.OtpResponse;
import com.api.distr.docs.sales.dto.SalesEntryDto;
import com.api.distr.docs.sales.dto.SmartRouteAssignItem;
import com.api.distr.docs.sales.repo.DeliveryStatusService;
import com.api.distr.docs.sales.repo.DeliveryMapService;
import com.api.distr.docs.sales.dto.DeliveryMapDTO;

@RestController
@RequestMapping("/api/delivery")
public class DeliveryController {

    private final DeliveryService deliveryService;
    public DeliveryController(DeliveryService deliveryService) {
        this.deliveryService = deliveryService;
    }

    @Autowired
    private DeliveryStatusService service;

    @Autowired
    private DeliveryMapService mapService;

    // ── Auth ──────────────────────────────────────────────────────────────────
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody DeliveryLoginRequest request) {
        try {
            DeliveryLoginResponse response = deliveryService.login(request.getMobileNumber());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid mobile number");
        }
    }

    // ── Delivery list / status ────────────────────────────────────────────────
    @GetMapping("/statusList")
    public List<DeliveryStatusDTO> filter(
            @RequestParam String fromDate,
            @RequestParam String toDate) {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        return service.getByUpdateDate(
                LocalDate.parse(fromDate, fmt),
                LocalDate.parse(toDate,   fmt));
    }

    @GetMapping("/map")
    public List<DeliveryMapDTO> getMapPoints(
            @RequestParam String fromDate,
            @RequestParam String toDate
    ) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        LocalDate from = LocalDate.parse(fromDate, formatter);
        LocalDate to   = LocalDate.parse(toDate, formatter);
        return mapService.getMapPoints(from, to);
    }

    @PostMapping("/delivery-status")
    public ResponseEntity<?> upsertDelivery(@RequestBody DeliveryStatus deliveryStatus) {
        try {
            String msg = deliveryService.upsertByPicklistNo(deliveryStatus);
            return ResponseEntity.ok(new ApiResponse(true, msg));
        } catch (RuntimeException ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse(false, ex.getMessage()));
        }
    }

    @GetMapping("/deliveryList")
    public List<SalesEntryDto> getPicklists(@RequestParam Map<String, String> filters) {
        if (filters.isEmpty()) throw new IllegalArgumentException("At least one filter required.");
        return deliveryService.getDeliveryList(filters);
    }

    // ── Agents ────────────────────────────────────────────────────────────────

    /** Original assign — used by SalesDetail page (single agent, picklist list, car info) */
    @PostMapping("/assign")
    public String assignDelivery(@RequestBody DeliveryRequest request) {
        return deliveryService.assignDeliveries(request);
    }

    /**
     * Smart Route assign — used by SmartRoute page.
     * Accepts an array of stops: [{ picklist_no, sequence, deliveryBoyId, deliveryBoyName, lat, lon, address }, ...]
     */
    @PostMapping("/smart-assign")
    public ResponseEntity<?> smartAssign(@RequestBody List<SmartRouteAssignItem> items) {
        try {
            String msg = deliveryService.assignSmartRoute(items);
            return ResponseEntity.ok(new ApiResponse(true, msg));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new ApiResponse(false, e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse(false, "Smart assign failed: " + e.getMessage()));
        }
    }

    @GetMapping("/allAgents")
    public List<DeliveryAgent> getAllAgents() {
        return deliveryService.getAllAgents();
    }

    @PutMapping("/agent/{id}")
    public ResponseEntity<?> updateAgent(@PathVariable Long id, @RequestBody DeliveryAgent request) {
        try {
            return ResponseEntity.ok(deliveryService.updateAgent(id, request));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to update agent: " + ex.getMessage()));
        }
    }

    @PostMapping("/agent")
    public ResponseEntity<?> createAgent(@RequestBody DeliveryAgent request) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(deliveryService.createAgent(request));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to create agent: " + ex.getMessage()));
        }
    }

    @DeleteMapping("/delete/{picklistNo}")
    public ResponseEntity<?> deleteDelivery(@PathVariable String picklistNo) {
        return ResponseEntity.ok(new ApiResponse(true, "" + deliveryService.deleteByPicklistNo(picklistNo)));
    }

    // ── OTP ───────────────────────────────────────────────────────────────────
    @PostMapping("/send-otp")
    public ResponseEntity<OtpResponse> generateOtp(@RequestBody OtpRequest request) {
        try {
            if (request.getMobile() == null || request.getPicklistNo() == null)
                return ResponseEntity.badRequest().body(new OtpResponse("", false));
            return ResponseEntity.ok(new OtpResponse("5678", true));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(new OtpResponse("", false));
        }
    }
}
