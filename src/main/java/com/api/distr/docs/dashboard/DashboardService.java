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

    public List<DeliveryDetailsDto> getDetails(String status) {
        return dashboardDao.getDeliveryDetails(status);
    }
}

