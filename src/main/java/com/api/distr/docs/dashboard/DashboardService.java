package com.api.distr.docs.dashboard;



import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DashboardService {

    private final DashboardDao dashboardDao;

    public DashboardService(DashboardDao dashboardDao) {
        this.dashboardDao = dashboardDao;
    }

    public DeliverySummaryDto getSummary(String boyId) {
        return dashboardDao.getDeliverySummary(boyId);
    }

    public OverallSummaryDto getOverallSummary(String boyId) {
        return dashboardDao.getOverallSummary(boyId);
    }

    public OverallReportDto getOverallReport(int month, int year) {
        java.time.LocalDate from = java.time.LocalDate.of(year, month, 1);
        java.time.LocalDate to   = from.withDayOfMonth(from.lengthOfMonth());
        return dashboardDao.getOverallReport(from, to);
    }

    public List<DeliveryDetailsDto> getDetails(String status) {
        return dashboardDao.getDeliveryDetails(status);
    }
}

