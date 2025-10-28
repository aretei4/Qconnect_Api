package com.api.distr.docs.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;


@Component
public class AppConfig {
    
    @Value("${server.port}")
    private String homeDirectory;
    
    @Bean
    public String homeDirPath() {
        return homeDirectory;
    }
    
    public void printInfo() {
        System.out.println("App Name: " + homeDirectory);
      //  System.out.println("Version: " + version);
    }
    // Usage example
    @Bean
    public String fileLocation() {
        return homeDirectory + "/app-data/";
    }
}
