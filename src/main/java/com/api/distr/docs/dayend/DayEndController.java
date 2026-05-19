package com.api.distr.docs.dayend;

import java.time.LocalDate;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/dayend")
public class DayEndController {

    @Autowired
    private DayEndService service;

    @Autowired
    private DayEndApprovalService approvalService;

    // 🚀 START — agent begins day-end process
    @PostMapping("/start")
    public ResponseEntity<String> start(@RequestBody DayEndDto dto) {
        approvalService.startDayEnd(dto);
        return ResponseEntity.ok("DayEnd Started");
    }

    // ✅ CREATE — agent submits final picklists + amount (moves STARTED → PENDING)
    @PostMapping("/create")
    public ResponseEntity<String> create(@RequestBody DayEndDto dto) {
        approvalService.createDayEnd(dto);
        return ResponseEntity.ok("DayEnd Created");
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
}