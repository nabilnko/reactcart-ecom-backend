package com.shophub.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // ✅ PUBLIC ENDPOINTS
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/api/products/**").permitAll()
                        .requestMatchers("/api/categories/**").permitAll()
                        .requestMatchers("/api/contact/**").permitAll()
                        .requestMatchers("/api/contact-messages").permitAll()

                        // ✅ ORDER ENDPOINTS
                        .requestMatchers("/api/orders").permitAll()  // Create order (guest + auth)
                        .requestMatchers("/api/orders/guest").permitAll()  // Guest checkout
                        .requestMatchers("/api/orders/{id}").permitAll()  // View receipt (guest + auth)
                        .requestMatchers("/api/orders/my-orders").permitAll()  // ✅ TEMPORARILY ALLOW - FIX LATER
                        .requestMatchers("/api/orders/admin/**").permitAll()  // ✅ TEMPORARILY ALLOW - FIX LATER

                        // ✅ USER ENDPOINTS
                        .requestMatchers("/api/users/profile").permitAll()  // ✅ TEMPORARILY ALLOW
                        .requestMatchers("/api/users/customers").permitAll()  // ✅ TEMPORARILY ALLOW
                        .requestMatchers("/api/users/count").permitAll()  // ✅ TEMPORARILY ALLOW

                        // All other requests require authentication
                        .anyRequest().permitAll()  // ✅ TEMPORARILY ALLOW ALL
                );

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList("http://localhost:3000"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);

        return source;
    }
}
