package com.api.distr.docs.sales;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.api.distr.docs.sales.dto.DeliveryAgent;
import com.api.distr.docs.sales.dto.DeliveryLoginResponse;
import com.api.distr.docs.sales.dto.DeliveryRequest;
import com.api.distr.docs.sales.dto.DeliveryStatus;
import com.api.distr.docs.sales.dto.SalesEntryDto;
import com.api.distr.docs.sales.dto.SmartRouteAssignItem;
import com.api.distr.docs.sales.repo.DeliveryRepository;
import com.api.distr.docs.upload.CustomerRepository;

@Service
public class DeliveryService {

    private static final Logger log = LoggerFactory.getLogger(DeliveryService.class);

    private final DeliveryRepository deliveryRepository;

    @Autowired
    CustomerRepository ccustRepo;

    public DeliveryService(DeliveryRepository deliveryRepository) {
        this.deliveryRepository = deliveryRepository;
    }

    public DeliveryLoginResponse login(String mobileNumber) {
        log.info("login: mobileNumber={}", mobileNumber);
        try {
            DeliveryLoginResponse response = deliveryRepository.findByMobile(mobileNumber);
            log.info("login: success for mobileNumber={}, deliveryId={}", mobileNumber, response.getDeliveryId());
            return response;
        } catch (Exception e) {
            log.error("login failed: mobileNumber={}, error={}", mobileNumber, e.getMessage(), e);
            throw e;
        }
    }

    @Transactional
    public String assignDeliveries(DeliveryRequest request) {
        log.info("assignDeliveries: deliveryBoyId={}, picklistCount={}",
                request.getDeliveryBoyId(),
                request.getPicklistNos() != null ? request.getPicklistNos().size() : 0);
        try {
            deliveryRepository.saveOrUpdate(request);
            log.info("assignDeliveries: completed for deliveryBoyId={}", request.getDeliveryBoyId());
            return "Delivery assignments updated successfully.";
        } catch (Exception e) {
            log.error("assignDeliveries failed: deliveryBoyId={}, error={}", request.getDeliveryBoyId(), e.getMessage(), e);
            throw e;
        }
    }

    /** Smart Route assign — accepts the per-stop array from SmartRoute.tsx */
    @Transactional
    public String assignSmartRoute(java.util.List<SmartRouteAssignItem> items) {
        if (items == null || items.isEmpty())
            throw new IllegalArgumentException("No stops provided");
        log.info("assignSmartRoute: {} stops", items.size());
        try {
            deliveryRepository.saveSmartRouteAssignments(items);
            log.info("assignSmartRoute: completed {} stops", items.size());
            return "Smart route assigned: " + items.size() + " stops";
        } catch (Exception e) {
            log.error("assignSmartRoute failed: stopCount={}, error={}", items.size(), e.getMessage(), e);
            throw e;
        }
    }

    @Transactional
    public String upsertByDireId(DeliveryStatus deliveryStatus) {
        log.info("upsertByDireId: direId={}, delivered={}, deliveryId={}",
                deliveryStatus.getDireId(), deliveryStatus.isDelivered(), deliveryStatus.getDelivery_id());
        try {
            deliveryRepository.upsertByDireId(deliveryStatus);

            if (deliveryStatus.getLat() != null && deliveryStatus.getLon() != null) {
                ccustRepo.updateCustomerLatLon(deliveryStatus);
            }

            log.info("upsertByDireId: completed for direId={}", deliveryStatus.getDireId());
            return "Delivery status updated successfully.";
        } catch (Exception e) {
            log.error("upsertByDireId failed: direId={}, error={}", deliveryStatus.getDireId(), e.getMessage(), e);
            throw e;
        }
    }

    public List<DeliveryAgent> getAllAgents() {
        log.info("getAllAgents");
        return deliveryRepository.findAll();
    }

    public DeliveryAgent createAgent(DeliveryAgent request) {
        if (request.getName() == null || request.getName().isBlank())
            throw new IllegalArgumentException("Agent name is required");
        if (request.getContact() == null || request.getContact().isBlank())
            throw new IllegalArgumentException("Mobile number is required");
        log.info("createAgent: name={}, contact={}", request.getName(), request.getContact());
        try {
            DeliveryAgent agent = deliveryRepository.createAgent(request);
            log.info("createAgent: created agent id={}", agent.getId());
            return agent;
        } catch (Exception e) {
            log.error("createAgent failed: name={}, error={}", request.getName(), e.getMessage(), e);
            throw e;
        }
    }

    public DeliveryAgent updateAgent(Long id, DeliveryAgent request) {
        if (request.getName() == null || request.getName().isBlank())
            throw new IllegalArgumentException("Agent name is required");
        if (request.getContact() == null || request.getContact().isBlank())
            throw new IllegalArgumentException("Mobile number is required");
        log.info("updateAgent: id={}, name={}", id, request.getName());
        try {
            DeliveryAgent agent = deliveryRepository.updateAgent(id, request);
            log.info("updateAgent: updated agent id={}", id);
            return agent;
        } catch (Exception e) {
            log.error("updateAgent failed: id={}, error={}", id, e.getMessage(), e);
            throw e;
        }
    }

    public int deleteByPicklistNo(String picklistNo) {
        log.info("deleteByPicklistNo: picklistNo={}", picklistNo);
        try {
            int rows = deliveryRepository.deleteByPicklistNo(picklistNo);
            log.info("deleteByPicklistNo: deleted {} row(s) for picklistNo={}", rows, picklistNo);
            return rows;
        } catch (Exception e) {
            log.error("deleteByPicklistNo failed: picklistNo={}, error={}", picklistNo, e.getMessage(), e);
            throw e;
        }
    }

    public int deleteByDireId(Long direId) {
        log.info("deleteByDireId: direId={}", direId);
        try {
            int rows = deliveryRepository.deleteByDireId(direId);
            log.info("deleteByDireId: deleted {} row(s) for direId={}", rows, direId);
            return rows;
        } catch (Exception e) {
            log.error("deleteByDireId failed: direId={}, error={}", direId, e.getMessage(), e);
            throw e;
        }
    }

    public int countPendingDans() {
        Integer count = deliveryRepository.countPendingDans();
        return count != null ? count : 0;
    }

    public List<SalesEntryDto> getDeliveryList(Map<String, String> filters) {
        log.info("getDeliveryList: filters={}", filters);
        String deliveryId   = filters.getOrDefault("delivery_id", "");
        String statusFilter = filters.getOrDefault("status", "pending");
        try {
            List<SalesEntryDto> result = statusFilter.equals("pending")
                ? deliveryRepository.getAllSales(deliveryId)
                : deliveryRepository.getAllSalesByStatus(deliveryId, statusFilter);
            log.info("getDeliveryList: returned {} entries", result.size());
            return result;
        } catch (Exception e) {
            log.error("getDeliveryList failed: filters={}, error={}", filters, e.getMessage(), e);
            throw e;
        }
    }

}

