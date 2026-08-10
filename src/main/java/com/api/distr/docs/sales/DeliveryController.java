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
import com.api.distr.docs.sales.dto.AssignmentDTO;
import com.api.distr.docs.sales.dto.SalesEntryDto;
import com.api.distr.docs.sales.dto.SmartRouteAssignItem;
import com.api.distr.docs.sales.repo.DeliveryStatusService;
import com.api.distr.docs.sales.repo.DeliveryMapService;
import com.api.distr.docs.sales.repo.DeliveryViolationService;
import com.api.distr.docs.sales.dto.DeliveryMapDTO;
import com.api.distr.docs.sales.dto.DeliveryViolationDTO;

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

    @Autowired
    private DeliveryViolationService violationService;

    // ── Auth ──────────────────────────────────────────────────────────────────
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody DeliveryLoginRequest request) {
        try {
            DeliveryLoginResponse response = deliveryService.login(request.getMobileNumber(), request.getPassword());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid credentials");
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

    /** Invoice Report — CLOSED (status 10) records only, optional payment mode filter. */
    @GetMapping("/invoiceReport")
    public List<DeliveryStatusDTO> invoiceReport(
            @RequestParam String fromDate,
            @RequestParam String toDate,
            @RequestParam(required = false) String paymentMode) {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        return service.getClosedByDate(
                LocalDate.parse(fromDate, fmt),
                LocalDate.parse(toDate,   fmt),
                paymentMode);
    }

    /**
     * GET /api/delivery/violations?fromDate=dd/MM/yyyy&toDate=dd/MM/yyyy
     *
     * Compares actual delivery location (delivery_status.lat/lon) against the
     * customer's registered address (customer_details.lat/lon).
     * Other details (invoice, net value) join from stage_sales_entery.
     * Falls back to sample-violations.json when no live data exists.
     */
    @GetMapping("/violations")
    public List<DeliveryViolationDTO> getViolations(
            @RequestParam String fromDate,
            @RequestParam String toDate) {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        return violationService.getViolations(
                LocalDate.parse(fromDate, fmt),
                LocalDate.parse(toDate,   fmt));
    }

    /** ASSIGNED (status 9) map points for one agent — coordinates from customer master. */
    @GetMapping("/map")
    public List<DeliveryMapDTO> getMapPoints(@RequestParam String deliveryId) {
        return mapService.getMapPoints(deliveryId);
    }

    @PostMapping("/delivery-status")
    public ResponseEntity<?> upsertDelivery(@RequestBody DeliveryStatus deliveryStatus) {
        try {
            String msg = deliveryService.upsertByDireId(deliveryStatus);
            return ResponseEntity.ok(new ApiResponse(true, msg));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse(false, ex.getMessage()));
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse(false, ex.getMessage() != null
                            ? ex.getMessage() : ex.getClass().getSimpleName()));
        }
    }

    @GetMapping("/deliveryList")
    public List<SalesEntryDto> getPicklists(@RequestParam Map<String, String> filters) {
        if (filters.isEmpty()) throw new IllegalArgumentException("At least one filter required.");
        return deliveryService.getDeliveryList(filters);
    }

    // ── Agents ────────────────────────────────────────────────────────────────

    /** Check if any DAN is pending before allowing dispatch. */
    @GetMapping("/pending-dan-check")
    public ResponseEntity<?> pendingDanCheck() {
        int count = deliveryService.countPendingDans();
        if (count > 0)
            return ResponseEntity.ok(new ApiResponse(false, count + " pending DAN(s) must be closed before dispatching."));
        return ResponseEntity.ok(new ApiResponse(true, "ok"));
    }

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

    /** Delete delivery assignment by dire_id (stage_sales_entery.dire_id). */
    @DeleteMapping("/delete/dire/{direId}")
    public ResponseEntity<?> deleteDeliveryBySalesId(@PathVariable Long direId) {
        try {
            int rows = deliveryService.deleteByDireId(direId);
            return ResponseEntity.ok(new ApiResponse(true, "" + rows));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(new ApiResponse(false, "Delete failed: " + e.getMessage()));
        }
    }

    // ── Assignments ───────────────────────────────────────────────────────────
    @GetMapping("/assignments")
    public ResponseEntity<List<AssignmentDTO>> getAssignments(
            @RequestParam(required = false) String fromDate,
            @RequestParam(required = false) String toDate,
            @RequestParam(required = false) String agentId,
            @RequestParam(required = false) Integer status) {
        return ResponseEntity.ok(
            deliveryService.getAllAssignments(fromDate, toDate, agentId, status));
    }

    /** Extracts dire ids from body — accepts single {"direId": 1} or multiple {"direIds": [1,2,3]}. */
    private static List<Long> extractDireIds(Map<String, Object> body) {
        List<Long> ids = new java.util.ArrayList<>();
        Object single = body.get("direId");
        if (single instanceof Number n) ids.add(n.longValue());
        Object multi = body.get("direIds");
        if (multi instanceof List<?> list) {
            for (Object o : list)
                if (o instanceof Number n) ids.add(n.longValue());
        }
        return ids;
    }

    /** Accept newly-assigned deliveries: status 9 → 0 (PENDING). Single direId or direIds array. */
    @PostMapping("/accept")
    public ResponseEntity<?> acceptAssignment(@RequestBody Map<String, Object> body) {
        List<Long> direIds = extractDireIds(body);
        if (direIds.isEmpty())
            return ResponseEntity.badRequest().body(new ApiResponse(false, "direId or direIds required"));
        try {
            String msg = deliveryService.acceptAssignments(direIds);
            return ResponseEntity.ok(new ApiResponse(true, msg));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(new ApiResponse(false, ex.getMessage()));
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse(false, "Accept failed: " + ex.getMessage()));
        }
    }

    /** Reject newly-assigned deliveries: status 9 → 8 (REJECTED). Single direId or direIds array. */
    @PostMapping("/reject-assignments")
    public ResponseEntity<?> rejectAssignments(@RequestBody Map<String, Object> body) {
        List<Long> direIds = extractDireIds(body);
        if (direIds.isEmpty())
            return ResponseEntity.badRequest().body(new ApiResponse(false, "direId or direIds required"));
        try {
            String msg = deliveryService.rejectAssignments(direIds);
            return ResponseEntity.ok(new ApiResponse(true, msg));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(new ApiResponse(false, ex.getMessage()));
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse(false, "Reject failed: " + ex.getMessage()));
        }
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
