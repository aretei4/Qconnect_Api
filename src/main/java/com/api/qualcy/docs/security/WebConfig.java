package com.api.qualcy.docs.security;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

//@Configuration
public class WebConfig implements WebMvcConfigurer {
	/*
	 * @Override public void addResourceHandlers(ResourceHandlerRegistry registry) {
	 * registry.addResourceHandler("/**")
	 * .addResourceLocations("classpath:/static/", "file:/custom/path/");
	 * System.out.println("   insidesecurityFilterChain "); }
	 * 
	 * @Override public void addCorsMappings(CorsRegistry registry) {
	 * registry.addMapping("/**") .allowedOrigins("*") // or your specific
	 * OnlyOffice origin .allowedMethods("POST"); }
	 */
}
