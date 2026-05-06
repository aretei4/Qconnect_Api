package com.api.distr.docs.security;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * MVC configuration.
 * @EnableWebMvc lives here (NOT on SecurityConfig) so Spring Security 6's
 * MvcRequestMatcher initialises correctly and permitAll() rules are honoured.
 *
 * CORS is handled at the Security level (SecurityConfig.corsConfigurationSource)
 * so preflight OPTIONS requests are never intercepted before authentication.
 */
@Configuration
@EnableWebMvc
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/static/", "file:/custom/path/");
    }
}
