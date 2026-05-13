package com.api.distr.docs.sales;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.api.distr.docs.sales.dto.DeliveryAgent;
import com.api.distr.docs.sales.dto.DeliveryLoginResponse;
import com.api.distr.docs.sales.dto.DeliveryRequest;
import com.api.distr.docs.sales.dto.DeliveryStatus;
import com.api.distr.docs.sales.dto.SalesEntry;
import com.api.distr.docs.sales.dto.SalesEntryDto;
import com.api.distr.docs.sales.repo.DeliveryRepository;
import com.api.distr.docs.upload.CustomerRepository;

@Service
public class DeliveryService {

    private final DeliveryRepository deliveryRepository;
   
    @Autowired
    CustomerRepository ccustRepo;
    
    public DeliveryService(DeliveryRepository deliveryRepository) {
        this.deliveryRepository = deliveryRepository;
    }

    public DeliveryLoginResponse login(String mobileNumber) {
        return deliveryRepository.findByMobile(mobileNumber);
    }
    
    public String assignDeliveries(DeliveryRequest request) {
        deliveryRepository.saveOrUpdate(request);
        return "Delivery assignments updated successfully.";
    }
    public String upsertByPicklistNo(DeliveryStatus deliveryStatus) {
        deliveryRepository.upsertByPicklistNo(deliveryStatus);
        ccustRepo.updateCustomerLatLon(deliveryStatus);
        return "Delivery assignments updated successfully.";
    }
    
    public List<DeliveryAgent> getAllAgents() {
        return deliveryRepository.findAll();
    }

    public DeliveryAgent createAgent(DeliveryAgent request) {
        if (request.getName() == null || request.getName().isBlank())
            throw new IllegalArgumentException("Agent name is required");
        if (request.getContact() == null || request.getContact().isBlank())
            throw new IllegalArgumentException("Mobile number is required");
        return deliveryRepository.createAgent(request);
    }
    public int deleteByPicklistNo(String picklistNo) {
    	return deliveryRepository.deleteByPicklistNo(picklistNo);
    }
    
    public List<SalesEntryDto> getDeliveryList(Map<String, String> filters) {
    	String value = "";
    	   for (Map.Entry<String, String> entry : filters.entrySet()) {
               String column = entry.getKey().toLowerCase();
               value = entry.getValue();              
           }
        return deliveryRepository.getAllSales(value);
    }
    
}

