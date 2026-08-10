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
import com.api.distr.docs.user.User;
import com.api.distr.docs.user.UserRepository;
import com.api.distr.docs.security.UserJwtService;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.security.crypto.password.PasswordEncoder;

@Service
public class DeliveryService {

    private static final Logger log = LoggerFactory.getLogger(DeliveryService.class);

    private final DeliveryRepository deliveryRepository;

    @Autowired
    CustomerRepository ccustRepo;

    @Autowired
    UserRepository userRepository;

    @Autowired
    PasswordEncoder passwordEncoder;

    @Autowired
    UserJwtService userJwtService;

    public DeliveryService(DeliveryRepository deliveryRepository) {
        this.deliveryRepository = deliveryRepository;
    }

    public DeliveryLoginResponse login(String mobileNumber, String password) {
        log.info("login: mobileNumber={}", mobileNumber);
        boolean otpLogin = (password == null || password.isBlank());

        if (otpLogin) {
            // OTP login: check users table FIRST so admin/manager always get their real role.
            // Try username match first, then phone match (users may store mobile in phone column).
            java.util.Optional<User> userOpt = userRepository.findByUsername(mobileNumber);
            if (!userOpt.isPresent()) {
                userOpt = userRepository.findByPhone(mobileNumber);
                if (userOpt.isPresent())
                    log.info("login(otp): matched by phone column for mobile={}", mobileNumber);
            }
            if (userOpt.isPresent()) {
                User u = userOpt.get();
                if (!u.isEnabled()) {
                    log.warn("login(otp): user account disabled: {}", mobileNumber);
                    throw new RuntimeException("Account is disabled");
                }
                log.info("login(otp): found in users table, userId={}, role={}", u.getId(), u.getRole());
                String displayName = (u.getFullName() != null && !u.getFullName().isBlank())
                        ? u.getFullName() : u.getUsername();
                String jwt = userJwtService.generateToken(u.getUsername(), "ROLE_" + u.getRole().name());
                return new DeliveryLoginResponse(u.getId(), displayName, 0, u.getRole().name(), jwt);
            }
            // Delivery agent (only in delivery_master, no users-table record)
            try {
                DeliveryLoginResponse r = deliveryRepository.findByMobile(mobileNumber);
                log.info("login(otp): found in delivery_master, deliveryId={}", r.getDeliveryId());
                return new DeliveryLoginResponse(r.getDeliveryId(), r.getDeliveryName(), r.getBuId(), "DELIVERY", null);
            } catch (EmptyResultDataAccessException e) {
                log.error("login(otp): mobile not found anywhere: {}", mobileNumber);
                throw new RuntimeException("Invalid credentials");
            }
        }

        // Password login: delivery_master first, users table as fallback
        try {
            DeliveryLoginResponse r = deliveryRepository.findByMobile(mobileNumber);
            log.info("login(pwd): found in delivery_master, deliveryId={}", r.getDeliveryId());
            return new DeliveryLoginResponse(r.getDeliveryId(), r.getDeliveryName(), r.getBuId(), "DELIVERY", null);
        } catch (EmptyResultDataAccessException notInDelivery) {
            log.info("login(pwd): mobile not found in delivery_master, trying users table");
        }

        User user = userRepository.findByUsername(mobileNumber)
                .orElseThrow(() -> {
                    log.error("login(pwd): mobile not found in users table either: {}", mobileNumber);
                    return new RuntimeException("Invalid credentials");
                });

        if (!user.isEnabled()) {
            log.warn("login(pwd): user account disabled: {}", mobileNumber);
            throw new RuntimeException("Account is disabled");
        }

        if (!passwordEncoder.matches(password, user.getPassword())) {
            log.warn("login(pwd): invalid password for user: {}", mobileNumber);
            throw new RuntimeException("Invalid credentials");
        }

        log.info("login(pwd): found in users table, userId={}", user.getId());
        String displayName = (user.getFullName() != null && !user.getFullName().isBlank())
                ? user.getFullName() : user.getUsername();
        String jwt = userJwtService.generateToken(user.getUsername(), "ROLE_" + user.getRole().name());
        return new DeliveryLoginResponse(user.getId(), displayName, 0, user.getRole().name(), jwt);
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

    @Transactional
    public String acceptAssignments(List<Long> direIds) {
        log.info("acceptAssignments: direIds={}", direIds);
        int rows = deliveryRepository.acceptAssignments(direIds);
        if (rows == 0)
            throw new IllegalArgumentException("No assignments found or already accepted: direIds=" + direIds);
        log.info("acceptAssignments: {} accepted", rows);
        return rows + " assignment(s) accepted";
    }

    @Transactional
    public String rejectAssignments(List<Long> direIds) {
        log.info("rejectAssignments: direIds={}", direIds);
        int rows = deliveryRepository.rejectAssignments(direIds);
        if (rows == 0)
            throw new IllegalArgumentException("No assignments found to reject: direIds=" + direIds);
        log.info("rejectAssignments: {} rejected", rows);
        return rows + " assignment(s) rejected";
    }

    public int countPendingDans() {
        Integer count = deliveryRepository.countPendingDans();
        return count != null ? count : 0;
    }

    public List<SalesEntryDto> getDeliveryList(Map<String, String> filters) {
        log.info("getDeliveryList: filters={}", filters);
        // accept both spellings: delivery_id and deliveri_id (legacy typo)
        String deliveryId   = filters.containsKey("delivery_id")
                            ? filters.get("delivery_id")
                            : filters.getOrDefault("deliveri_id", "");
        String statusFilter = filters.getOrDefault("status", "pending");
        try {
            // Always use getAllSalesByStatus so pending includes status 9 (newly assigned)
            List<SalesEntryDto> result = deliveryRepository.getAllSalesByStatus(deliveryId, statusFilter);
            log.info("getDeliveryList: returned {} entries", result.size());
            return result;
        } catch (Exception e) {
            log.error("getDeliveryList failed: filters={}, error={}", filters, e.getMessage(), e);
            throw e;
        }
    }

    public List<com.api.distr.docs.sales.dto.AssignmentDTO> getAllAssignments(
            String fromDate, String toDate, String agentId, Integer status) {
        return deliveryRepository.getAllAssignments(fromDate, toDate, agentId, status);
    }

}

