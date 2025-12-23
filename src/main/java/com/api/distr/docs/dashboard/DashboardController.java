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
    public DeliverySummaryDto getDeliverySummary() {
        return dashboardService.getSummary();
    }

    @GetMapping("/deliveries")
    public List<DeliveryDetailsDto> getDeliveryDetails(
            @RequestParam String status) {
        return dashboardService.getDetails(status);
    }
}

