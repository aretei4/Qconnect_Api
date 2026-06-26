package com.api.distr.docs.upload;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/customer")
public class CustomerController {

    private final CustomerRepository customerRepository;

    public CustomerController(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    /**
     * PATCH /api/customer/{custNo}/location
     * Body: { "lat": 20.4635, "lon": 85.8835 }
     *
     * Updates lat/lon of a customer in customer_details by cust_no.
     */
    @PatchMapping("/{custNo}/location")
    public ResponseEntity<?> updateLocation(
            @PathVariable String custNo,
            @RequestBody Map<String, Double> body) {

        Double lat = body.get("lat");
        Double lon = body.get("lon");

        if (lat == null || lon == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", "lat and lon are required"));
        }

        int rows = customerRepository.updateLatLon(custNo, lat, lon);
        if (rows == 0) {
            return ResponseEntity.status(404)
                    .body(Map.of("success", false, "message", "Customer not found: " + custNo));
        }
        return ResponseEntity.ok(Map.of("success", true, "message", "Location updated", "rows", rows));
    }
}
