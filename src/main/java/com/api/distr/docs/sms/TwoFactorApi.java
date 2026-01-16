package com.api.distr.docs.sms;

import com.google.gson.JsonObject;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;

public interface TwoFactorApi {

    @POST("API/V1/{apiKey}/ADDON_SERVICES/SEND/TSMS")
    Call<JsonObject> sendTransactionalSms(
            @Path("apiKey") String apiKey,
            @Body JsonObject request
    );
    @GET("API/V1/{apiKey}/SMS/{mobile}/{otp}")
    Call<JsonObject> sendOtp(
            @Path("apiKey") String apiKey,
            @Path("mobile") String mobile,
            @Path("otp") String otp
    );

    @GET("API/V1/{apiKey}/SMS/{mobile}/AUTOGEN")
    Call<JsonObject> sendAutoOtp(
            @Path("apiKey") String apiKey,
            @Path("mobile") String mobile
    );
}

