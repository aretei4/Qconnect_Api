package com.api.distr.docs.dashboard;


import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/delivery-summary")
    public DeliverySummaryDto getDeliverySummary(
            @RequestParam(required = false) String boyId
    ) {
    	System.out.println(" Boy id is ******************  "+boyId);
        return dashboardService.getSummary(boyId);
    }

    @GetMapping("/overall-summary")
    public OverallSummaryDto getOverallSummary(
            @RequestParam(required = false) String boyId
    ) {
        return dashboardService.getOverallSummary(boyId);
    }

    /** Overall report for a month — from payment_details + stage_sales_entery. */
    @GetMapping("/overall-report")
    public OverallReportDto getOverallReport(
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year) {
        java.time.LocalDate now = java.time.LocalDate.now();
        return dashboardService.getOverallReport(
                month != null ? month : now.getMonthValue(),
                year  != null ? year  : now.getYear());
    }

    @GetMapping("/deliveries")
    public List<DeliveryDetailsDto> getDeliveryDetails(
            @RequestParam String status) {
        return dashboardService.getDetails(status);
    }
}

