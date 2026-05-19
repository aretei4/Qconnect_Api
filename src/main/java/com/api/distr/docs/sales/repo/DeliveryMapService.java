package com.api.distr.docs.sales.repo;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import org.springframework.stereotype.Service;

import com.api.distr.docs.sales.dto.DeliveryMapDTO;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class DeliveryMapService {

    private final ObjectMapper objectMapper = new ObjectMapper();

    // Static delivery points around Bhubaneswar — replace with DB query once lat/lon columns are added
    // All stops are PENDING — this is the planned route view based on delivery_assignments.
    // Status will be updated to DELIVERED/FAILED once the agent submits from the mobile app.
    private static final String STATIC_JSON = """
            [
              { "picklist_no": "E587P22657", "deliveryBoyName": "Ravi Kumar",  "status": "PENDING", "lat": 20.2961, "lon": 85.8245, "delivery_date": "19/05/2026", "address": "Rajmahal Square, Bhubaneswar",    "sequence": 1 },
              { "picklist_no": "E587P22659", "deliveryBoyName": "Ravi Kumar",  "status": "PENDING", "lat": 20.2721, "lon": 85.8411, "delivery_date": "19/05/2026", "address": "Saheed Nagar, Bhubaneswar",       "sequence": 2 },
              { "picklist_no": "E587P22665", "deliveryBoyName": "Ravi Kumar",  "status": "PENDING", "lat": 20.2560, "lon": 85.8480, "delivery_date": "19/05/2026", "address": "Satya Nagar, Bhubaneswar",        "sequence": 3 },
              { "picklist_no": "E587P22669", "deliveryBoyName": "Ravi Kumar",  "status": "PENDING", "lat": 20.2490, "lon": 85.8170, "delivery_date": "19/05/2026", "address": "Jagamara, Bhubaneswar",           "sequence": 4 },
              { "picklist_no": "E587P22673", "deliveryBoyName": "Ravi Kumar",  "status": "PENDING", "lat": 20.2670, "lon": 85.8730, "delivery_date": "19/05/2026", "address": "Nayapalli, Bhubaneswar",          "sequence": 5 },

              { "picklist_no": "E587P22658", "deliveryBoyName": "Suresh Jena", "status": "PENDING", "lat": 20.3085, "lon": 85.8382, "delivery_date": "19/05/2026", "address": "Bapuji Nagar, Bhubaneswar",       "sequence": 1 },
              { "picklist_no": "E587P22661", "deliveryBoyName": "Suresh Jena", "status": "PENDING", "lat": 20.2830, "lon": 85.8560, "delivery_date": "19/05/2026", "address": "IRC Village, Bhubaneswar",        "sequence": 2 },
              { "picklist_no": "E587P22666", "deliveryBoyName": "Suresh Jena", "status": "PENDING", "lat": 20.3010, "lon": 85.7980, "delivery_date": "19/05/2026", "address": "Patia, Bhubaneswar",              "sequence": 3 },
              { "picklist_no": "E587P22670", "deliveryBoyName": "Suresh Jena", "status": "PENDING", "lat": 20.3440, "lon": 85.8360, "delivery_date": "19/05/2026", "address": "Chandrasekharpur, Bhubaneswar",   "sequence": 4 },
              { "picklist_no": "E587P22674", "deliveryBoyName": "Suresh Jena", "status": "PENDING", "lat": 20.3520, "lon": 85.8450, "delivery_date": "19/05/2026", "address": "Infocity, Bhubaneswar",           "sequence": 5 },

              { "picklist_no": "E587P22660", "deliveryBoyName": "Manoj Nayak", "status": "PENDING", "lat": 20.3210, "lon": 85.8156, "delivery_date": "19/05/2026", "address": "Damana Square, Bhubaneswar",      "sequence": 1 },
              { "picklist_no": "E587P22663", "deliveryBoyName": "Manoj Nayak", "status": "PENDING", "lat": 20.2640, "lon": 85.8320, "delivery_date": "19/05/2026", "address": "Kalpana Square, Bhubaneswar",     "sequence": 2 },
              { "picklist_no": "E587P22667", "deliveryBoyName": "Manoj Nayak", "status": "PENDING", "lat": 20.2780, "lon": 85.8620, "delivery_date": "19/05/2026", "address": "Bomikhal, Bhubaneswar",           "sequence": 3 },
              { "picklist_no": "E587P22671", "deliveryBoyName": "Manoj Nayak", "status": "PENDING", "lat": 20.2900, "lon": 85.8720, "delivery_date": "19/05/2026", "address": "Rasulgarh, Bhubaneswar",          "sequence": 4 },
              { "picklist_no": "E587P22675", "deliveryBoyName": "Manoj Nayak", "status": "PENDING", "lat": 20.2410, "lon": 85.8390, "delivery_date": "19/05/2026", "address": "Lingaraj Nagar, Bhubaneswar",     "sequence": 5 },

              { "picklist_no": "E587P22662", "deliveryBoyName": "Arjun Sahoo", "status": "PENDING", "lat": 20.3150, "lon": 85.8470, "delivery_date": "19/05/2026", "address": "Niladri Vihar, Bhubaneswar",      "sequence": 1 },
              { "picklist_no": "E587P22664", "deliveryBoyName": "Arjun Sahoo", "status": "PENDING", "lat": 20.3380, "lon": 85.8240, "delivery_date": "19/05/2026", "address": "Kalinga Nagar, Bhubaneswar",      "sequence": 2 },
              { "picklist_no": "E587P22668", "deliveryBoyName": "Arjun Sahoo", "status": "PENDING", "lat": 20.3260, "lon": 85.8550, "delivery_date": "19/05/2026", "address": "Airport Road, Bhubaneswar",       "sequence": 3 },
              { "picklist_no": "E587P22672", "deliveryBoyName": "Arjun Sahoo", "status": "PENDING", "lat": 20.3100, "lon": 85.8050, "delivery_date": "19/05/2026", "address": "Nandankanan Road, Bhubaneswar",   "sequence": 4 },
              { "picklist_no": "E587P22676", "deliveryBoyName": "Arjun Sahoo", "status": "PENDING", "lat": 20.3300, "lon": 85.8680, "delivery_date": "19/05/2026", "address": "Baramunda, Bhubaneswar",          "sequence": 5 }
            ]
            """;

    public List<DeliveryMapDTO> getMapPoints(LocalDate fromDate, LocalDate toDate) {
        try {
            DeliveryMapDTO[] points = objectMapper.readValue(STATIC_JSON, DeliveryMapDTO[].class);
            return Arrays.asList(points);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse delivery map data: " + e.getMessage(), e);
        }
    }
}
