package com.api.qualcy.docs.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.github.cdimascio.dotenv.Dotenv;
import jakarta.annotation.PostConstruct;

@Configuration
public class AppConfig {
    
    @Value("${user.home}")
    private String homeDirectory;
    
    @Bean
    public String homeDirPath() {
        return homeDirectory;
    }
    
    @PostConstruct
    public void loadEnv() {
        Dotenv dotenv = Dotenv.configure().load();
        
        // Set as system properties (optional)
        dotenv.entries().forEach(entry -> 
            System.setProperty(entry.getKey(), entry.getValue())
        );
    }
    // Usage example
    @Bean
    public String fileLocation() {
        return homeDirectory + "/app-data/";
    }
}
