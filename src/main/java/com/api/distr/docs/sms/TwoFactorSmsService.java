package com.api.distr.docs.sms;

import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.google.gson.JsonObject;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

@Service
public class TwoFactorSmsService {

    @Value("${sms.twofactor.api-key}")
    private String apiKey;

    @Value("${sms.twofactor.sender-id}")
    private String senderId;

    @Value("${sms.twofactor.template-name}")
    private String templateName;

    private final TwoFactorApi api;

    public TwoFactorSmsService(TwoFactorApi api) {
        this.api = api;
    }
    
   
    public void sendOtpAsync(String mobile, String otp) {

        api.sendAutoOtp(apiKey, "91" + mobile)
              .enqueue(new retrofit2.Callback<>() {

                  @Override
                  public void onResponse(
                          Call<JsonObject> call,
                          retrofit2.Response<JsonObject> response) {

                      System.out.println("OTP Sent: " + response.body());
                  }

                  @Override
                  public void onFailure(
                          Call<JsonObject> call,
                          Throwable t) {

                      System.err.println("OTP Error: " + t.getMessage());
                  }
              });
    }


    public void sendAsync(
            String mobile,
            Map<String, String> vars
    ) {

        JsonObject body = new JsonObject();
        body.addProperty("From", senderId);
        body.addProperty("To", "91" + mobile);
        body.addProperty("TemplateName", templateName);

        vars.forEach(body::addProperty);

        api.sendTransactionalSms(apiKey, body)
           .enqueue(new Callback<>() {

               @Override
               public void onResponse(
                       Call<JsonObject> call,
                       Response<JsonObject> response) {

                   if (response.isSuccessful()) {
                       System.out.println("SMS Sent: " + response.body());
                   } else {
                       System.err.println("SMS Failed: " + response.errorBody());
                   }
               }

               @Override
               public void onFailure(
                       Call<JsonObject> call,
                       Throwable t) {

                   System.err.println("SMS Error: " + t.getMessage());
               }
           });
    }
}

