package com.api.distr.docs.security;

import org.springframework.beans.factory.annotation.Value;

public class AppServiceConfig {

    @Value("${JWT_SECRET}")
    private String jwtSecret;

    @Value("${DB_URL}")
    private String dbUrl;
    
   
}