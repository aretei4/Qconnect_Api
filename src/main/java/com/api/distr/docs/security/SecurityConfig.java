package com.api.distr.docs.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * NOTE: @EnableWebMvc is intentionally NOT here.
 * Placing @EnableWebMvc on SecurityConfig conflicts with Spring Security 6's
 * MvcRequestMatcher / HandlerMappingIntrospector and causes 403 on permitAll() routes.
 * MVC configuration lives in WebConfig.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;

    public SecurityConfig(JwtAuthFilter jwtAuthFilter) {
        this.jwtAuthFilter = jwtAuthFilter;
    }

    // ── Password encoder (shared across the app) ──────────────────────────────

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    // ── CORS — lives here so Security processes preflight before MVC ──────────

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration cfg = new CorsConfiguration();
        cfg.setAllowedOriginPatterns(List.of("*"));
        cfg.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        cfg.setAllowedHeaders(List.of("*"));
        cfg.setExposedHeaders(List.of("Authorization"));
        cfg.setAllowCredentials(false);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", cfg);
        return source;
    }

    // ── Security filter chain ─────────────────────────────────────────────────

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth

                // AntPathRequestMatcher — no MVC introspection dependency
                .requestMatchers(new AntPathRequestMatcher("/**", "OPTIONS")).permitAll()
                .requestMatchers(new AntPathRequestMatcher("/api/auth/**")).permitAll()

                // User management
                .requestMatchers(new AntPathRequestMatcher("/api/users/**", "GET"))
                    .hasAnyRole("ADMIN", "MANAGER")
                .requestMatchers(new AntPathRequestMatcher("/api/users/**", "POST"))
                    .hasRole("ADMIN")
                .requestMatchers(new AntPathRequestMatcher("/api/users/**", "PUT"))
                    .hasRole("ADMIN")
                .requestMatchers(new AntPathRequestMatcher("/api/users/**", "DELETE"))
                    .hasRole("ADMIN")

                // All existing endpoints stay public
                .anyRequest().permitAll()
            )
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
