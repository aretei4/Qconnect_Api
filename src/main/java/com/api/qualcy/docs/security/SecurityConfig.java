package com.api.qualcy.docs.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

@Configuration
@EnableWebMvc
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    	System.out.println("   insidesecurityFilterChain ");
        /*http
            .authorizeHttpRequests(auth -> auth
                // Allow access to static resources
                .requestMatchers(
                    "/", 
                    "/index.html", 
                    "/js/**", 
                    "/css/**", 
                    "/images/**",
                    "/favicon.ico"
                ).permitAll()
                
                // Secure other endpoints
                .anyRequest().authenticated()
            )
            
            // Optional: Configure form login
            .formLogin(form -> form
                .loginPage("/login")
                .permitAll()
            )
            
            // Optional: Configure logout
            .logout(logout -> logout
                .permitAll()
            );
*/
    	http
        .authorizeHttpRequests(auth -> auth
            .anyRequest().permitAll() // Allow access to all endpoints
        );
     //   .csrf(csrf -> csrf.en()); // Disable CSRF for simplicity
        return http.build();
    }
}