package com.api.distr.docs.sms;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/sms/webhook")
public class TwoFactorDlrController {

    @PostMapping("/dlr")
    public ResponseEntity<String> receiveDlr(
            @RequestBody Map<String, Object> payload) {

        String messageId = (String) payload.get("MessageId");
        String mobile = (String) payload.get("Mobile");
        String status = (String) payload.get("Status");

        System.out.println("DLR Received");
        System.out.println("MessageId: " + messageId);
        System.out.println("Mobile: " + mobile);
        System.out.println("Status: " + status);

        // TODO: update DB status
        return ResponseEntity.ok("OK");
    }
}

