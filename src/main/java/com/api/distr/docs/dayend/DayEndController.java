package com.api.distr.docs.dayend;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.api.distr.docs.sales.dto.PicklistUpdateRequest;

@RestController
@RequestMapping("/api/dayend")
public class DayEndController {

    @Autowired
    private DayEndService service;

    @Autowired
    private DayEndApprovalService approvalService;

    @Autowired
    private DayEndPicklistService picklistService;

    // 🚀 START — agent begins day-end process
    @PostMapping("/start")
    public ResponseEntity<Map<String, Object>> start(@RequestBody DayEndDto dto) {
        try {
            approvalService.startDayEnd(dto);
            return ResponseEntity.ok(Map.of("success", true, "message", "Day started successfully"));
        } catch (IllegalArgumentException e) {
            System.err.println("[DayEnd/start] Bad request: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("success", false, "message", e.getMessage()));
        } catch (Exception e) {
            System.err.println("[DayEnd/start] Error: " + e.getClass().getSimpleName() + " — " + e.getMessage());
            e.printStackTrace();
            String msg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", msg));
        }
    }

    // ✅ CREATE — agent submits final picklists + amount (moves STARTED → PENDING)
    @PostMapping("/create")
    public ResponseEntity<Map<String, Object>> create(@RequestBody DayEndDto dto) {
        try {
            approvalService.createDayEnd(dto);
            return ResponseEntity.ok(Map.of("success", true, "message", "Day End submitted successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    // ✅ APPROVE BY ID
    @PostMapping("/approve")
    public ResponseEntity<String> approve(@RequestBody DayEndDto dto) {
        approvalService.approveDayEndById(dto);
        return ResponseEntity.ok("DayEnd Approved");
    }

    // ❌ REJECT BY ID
    @PostMapping("/reject")
    public ResponseEntity<String> reject(@RequestBody DayEndDto dto) {
        approvalService.rejectDayEndById(dto);
        return ResponseEntity.ok("DayEnd Rejected");
    }

    // 🔍 LIST
    @GetMapping("/dayEndSummery")
    public ResponseEntity<List<DayEndResponseDto>> get(
            @RequestParam(required = false) Long deliveryId,
            @RequestParam(required = false)
            @DateTimeFormat(pattern = "dd-MM-yyyy") LocalDate fromDate,
            @RequestParam(required = false)
            @DateTimeFormat(pattern = "dd-MM-yyyy") LocalDate toDate
    ) {

        if (toDate == null) {
            toDate = LocalDate.now();
        }

        if (fromDate == null) {
            fromDate = toDate.minusDays(15);
        }

        return ResponseEntity.ok(
                approvalService.getDayEndList(deliveryId, fromDate, toDate)
        );
    }

    // 📊 SUMMARY
    @GetMapping("/summary")
    public ResponseEntity<DayEndSummary> getSummary(
            @RequestParam("date")
            @DateTimeFormat(pattern = "dd-MM-yyyy") LocalDate date,
            @RequestParam(value = "deliveryId", required = false) Long deliveryId
    ) {
        return ResponseEntity.ok(service.getDayEndSummary(date, deliveryId));
    }

    // ── Picklist management (Day-End screen) ──────────────────────────────────

    /** All picklists for a day-end record with payment detail — for Day-End review. */
    @GetMapping("/picklists/{dayendId}")
    public ResponseEntity<?> getPicklistsByDayendId(@PathVariable Long dayendId) {
        try {
            return ResponseEntity.ok(picklistService.getPicklistsByDayendId(dayendId));
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to load picklists: " + ex.getMessage());
        }
    }

    /** Update payment amount, mode, and delivery status for a single picklist. */
    @PutMapping("/picklist/{picklistNo}")
    public ResponseEntity<?> updatePicklist(
            @PathVariable String picklistNo,
            @RequestBody PicklistUpdateRequest req) {
        try {
            picklistService.updatePicklistPayment(picklistNo, req);
            return ResponseEntity.ok("Picklist updated successfully");
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Update failed: " + ex.getMessage());
        }
    }

    /** Remove a picklist from delivery assignments. */
    @DeleteMapping("/picklist/{picklistNo}")
    public ResponseEntity<?> deletePicklist(@PathVariable String picklistNo) {
        try {
            int rows = picklistService.deletePicklist(picklistNo);
            return ResponseEntity.ok(rows + " record(s) deleted");
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Delete failed: " + ex.getMessage());
        }
    }
}