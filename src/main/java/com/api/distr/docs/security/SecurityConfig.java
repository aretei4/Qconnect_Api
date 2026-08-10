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

    /**
     * app.security.enforce=true  → every endpoint requires a valid JWT except the whitelist below
     * app.security.enforce=false → legacy behaviour: only /api/users/** is protected
     */
    @org.springframework.beans.factory.annotation.Value("${app.security.enforce:false}")
    private boolean enforceAuth;

    /** Endpoints that must stay public even in enforce mode. */
    private static final String[] PUBLIC_PATHS = {
        // Web login flow
        "/api/auth/**",
        "/api/company/**",
        // Mobile delivery-agent app (no web JWT)
        "/api/delivery/login",
        "/api/delivery/send-otp",
        "/api/delivery/deliveryList",
        "/api/delivery/delivery-status",
        "/api/delivery/accept",
        "/api/delivery/reject-assignments",
        "/api/dayend/start",
        "/api/dayend/create",
        "/api/dayend/summary",
        "/api/dan/returns/**",
        "/api/dan/pamt-check",
        // Mobile DAN-close web view (opened via encrypted link, no JWT)
        "/api/mobile/dan/**",
    };

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
            .authorizeHttpRequests(auth -> {
                // AntPathRequestMatcher — no MVC introspection dependency
                auth.requestMatchers(new AntPathRequestMatcher("/**", "OPTIONS")).permitAll();
                for (String path : PUBLIC_PATHS) {
                    auth.requestMatchers(new AntPathRequestMatcher(path)).permitAll();
                }

                // User management — always role-protected
                auth.requestMatchers(new AntPathRequestMatcher("/api/users/**", "GET"))
                        .hasAnyRole("ADMIN", "MANAGER")
                    .requestMatchers(new AntPathRequestMatcher("/api/users/**", "POST"))
                        .hasRole("ADMIN")
                    .requestMatchers(new AntPathRequestMatcher("/api/users/**", "PUT"))
                        .hasRole("ADMIN")
                    .requestMatchers(new AntPathRequestMatcher("/api/users/**", "DELETE"))
                        .hasRole("ADMIN");

                // Everything else: JWT required when enforcement is on, public otherwise
                if (enforceAuth) auth.anyRequest().authenticated();
                else             auth.anyRequest().permitAll();
            })
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
