package com.api.distr.docs.violation;

import com.api.distr.docs.violation.dto.ViolationRowDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * GET /api/violation/rows?fromDate=dd/MM/yyyy&toDate=dd/MM/yyyy
 *
 * Returns flat delivery rows for the deviation tracker dashboard.
 * Client groups by custNo and sorts dates to build the ordinal matrix.
 */
@RestController
@RequestMapping("/api/violation")
public class ViolationController {

    private static final Logger log = LoggerFactory.getLogger(ViolationController.class);
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @Autowired
    private ViolationService violationService;

    @GetMapping("/rows")
    public ResponseEntity<List<ViolationRowDTO>> getRows(
            @RequestParam String fromDate,
            @RequestParam String toDate) {

        log.info("GET /api/violation/rows fromDate={} toDate={}", fromDate, toDate);
        LocalDate from = LocalDate.parse(fromDate, FMT);
        LocalDate to   = LocalDate.parse(toDate,   FMT);
        return ResponseEntity.ok(violationService.getRows(from, to));
    }
}
