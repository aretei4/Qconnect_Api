package com.api.distr.docs.sales;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.api.distr.docs.sales.dto.DeliveryAgent;
import com.api.distr.docs.sales.dto.DeliveryRequest;
import com.api.distr.docs.sales.dto.DeliveryStatus;
import com.api.distr.docs.sales.dto.SalesEntry;
import com.api.distr.docs.sales.repo.DeliveryRepository;

@Service
public class DeliveryService {

    private final DeliveryRepository deliveryRepository;

    public DeliveryService(DeliveryRepository deliveryRepository) {
        this.deliveryRepository = deliveryRepository;
    }

    public String assignDeliveries(DeliveryRequest request) {
        deliveryRepository.saveOrUpdate(request);
        return "Delivery assignments updated successfully.";
    }
    public String upsertByPicklistNo(DeliveryStatus deliveryStatus) {
        deliveryRepository.upsertByPicklistNo(deliveryStatus);
        return "Delivery assignments updated successfully.";
    }
    
    public List<DeliveryAgent> getAllAgents() {
        return deliveryRepository.findAll();
    }
    
    public List<SalesEntry> getDeliveryList(Map<String, String> filters) {
        return deliveryRepository.getAllSales();
    }
    
}

