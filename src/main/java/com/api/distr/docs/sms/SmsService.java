package com.api.distr.docs.sms;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.PublishRequest;
import software.amazon.awssdk.services.sns.model.SnsException;

@Service
@RequiredArgsConstructor
public class SmsService {

    private  SnsClient snsClient;

    public String sendSms(String mobile, String message) {
        try {
            PublishRequest request = PublishRequest.builder()
                    .phoneNumber("+91" + mobile)
                    .message(message)
                    .build();

            snsClient.publish(request);
            return "SMS sent successfully";
        } catch (SnsException e) {
            return "Error sending SMS: " + e.awsErrorDetails().errorMessage();
        }
    }
}

