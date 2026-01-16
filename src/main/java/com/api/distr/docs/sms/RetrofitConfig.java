package com.api.distr.docs.sms;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;



@Configuration
public class RetrofitConfig {

    @Value("${sms.twofactor.base-url}")
    private String baseUrl;

    @Bean
    public TwoFactorApi twoFactorApi() {

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(baseUrl)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        return retrofit.create(TwoFactorApi.class);
    }
}

