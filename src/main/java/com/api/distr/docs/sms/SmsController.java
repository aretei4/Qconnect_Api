package com.api.distr.docs.sms;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/sms")
@RequiredArgsConstructor
public class SmsController {
	 @Autowired
    private  SmsService smsService;

	 @Autowired
	    private  TwoFactorSmsService twoSmsService;
	 
    @PostMapping("/send")
    public String sendSms(@RequestBody SmsRequest request) {
        return smsService.sendSms(request.getMobile(), request.getMessage());
    }
    
    @PostMapping("/sendSms")
    public  ResponseEntity<?> sendTwoFactorSms(@RequestBody SmsRequest request) {
    	Map<String, String> vars = new HashMap<>();
    	vars.put("VAR1", "Subash");
    	vars.put("VAR2", "ORD12345");
    	vars.put("VAR3", "14-01-2026");
    	vars.put("VAR4", "Delivered");
    	twoSmsService.sendOtpAsync(request.getMobile(), "5678");
    	//twoSmsService.sendAsync(request.getMobile(), vars);
    	 return ResponseEntity.ok(
                 Map.of(
                         "status", "ACCEPTED",
                         "message", "SMS request queued successfully"
                 )
         );
       // return smsService.sendSms(request.getMobile(), request.getMessage());
    }
    
}
