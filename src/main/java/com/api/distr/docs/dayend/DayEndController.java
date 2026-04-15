package com.api.distr.docs.dayend;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dayend")
public class DayEndController {

    @Autowired
    private DayEndService service;
    
    @Autowired
    private DayEndApprovalService aprovalService;

 // ✅ CREATE
    @PostMapping("/create")
    public ResponseEntity<String> create(@RequestBody DayEndDto dto) {
    	aprovalService.createDayEnd(dto);
        return ResponseEntity.ok("DayEnd Created");
    }

    // ✅ APPROVE
    @PostMapping("/approve")
    public ResponseEntity<String> approve(@RequestBody DayEndDto dto) {
    	aprovalService.approveDayEnd(dto);
        return ResponseEntity.ok("DayEnd Approved");
    }

    // ❌ REJECT
    @PostMapping("/reject")
    public ResponseEntity<String> reject(@RequestBody DayEndDto dto) {
    	aprovalService.rejectDayEnd(dto);
        return ResponseEntity.ok("DayEnd Rejected");
    }

 
    @GetMapping("/get")
    public ResponseEntity<DayEndResponseDto> get(
            @RequestParam Long deliveryId,
            @RequestParam @DateTimeFormat(pattern = "dd-MM-yyyy") LocalDate date
    ) {
        DayEndDto dto = new DayEndDto();
        dto.setDeliveryId(deliveryId);
        dto.setDate(date.format(DateTimeFormatter.ofPattern("dd-MM-yyyy")));

        return ResponseEntity.ok(aprovalService.getDayEnd(dto));
    }
    
    @GetMapping("/summary")
    public ResponseEntity<DayEndSummary> getSummary(
            @RequestParam("date")
            @DateTimeFormat(pattern = "dd-MM-yyyy") LocalDate date,

            @RequestParam(value = "deliveryId", required = false) Long deliveryId
    ) {
        return ResponseEntity.ok(service.getDayEndSummary(date, deliveryId));
    }
    
    
}
