package com.shophub.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.Arrays;
import java.util.List;

@Configuration
public class CorsConfig {

    @Bean
    public CorsFilter corsFilter() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        CorsConfiguration config = new CorsConfiguration();

        // ✅ Allow credentials (cookies, authorization headers)
        config.setAllowCredentials(true);

        // ✅ Allow frontend origin
        config.setAllowedOrigins(List.of("http://localhost:3000"));

        // ✅ Allow all headers
        config.addAllowedHeader("*");

        // ✅ Allow all HTTP methods
        config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));

        // ✅ Expose headers that frontend can read
        config.setExposedHeaders(Arrays.asList("Authorization", "Content-Type"));

        // ✅ Apply CORS config to all endpoints
        source.registerCorsConfiguration("/**", config);

        return new CorsFilter(source);
    }
}
