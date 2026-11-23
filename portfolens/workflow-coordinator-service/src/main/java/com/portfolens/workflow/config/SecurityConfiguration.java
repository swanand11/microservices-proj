package com.portfolens.workflow.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.portfolens.workflow.controller.ApiError;
import java.time.Instant;
import java.util.Map;
import org.springframework.http.MediaType;

@Configuration
public class SecurityConfiguration {
    @Bean CorsConfigurationSource corsConfigurationSource(@Value("${app.cors.allowed-origins}") String origins) {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(java.util.Arrays.stream(origins.split(",")).map(String::trim).toList());
        config.setAllowedMethods(java.util.List.of("GET", "POST", "OPTIONS"));
        config.setAllowedHeaders(java.util.List.of("Authorization", "Content-Type", "Idempotency-Key"));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource(); source.registerCorsConfiguration("/**", config); return source;
    }
    @Bean SecurityFilterChain security(HttpSecurity http, JwtFilter jwt, ObjectMapper mapper) throws Exception {
        return http.csrf(csrf -> csrf.disable()).cors(cors -> {}).sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(a -> a.requestMatchers("/actuator/health/**", "/actuator/info").permitAll().anyRequest().authenticated())
                .exceptionHandling(e -> e.authenticationEntryPoint((request, response, cause) -> {
                    response.setStatus(401); response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                    mapper.writeValue(response.getOutputStream(), new ApiError(Instant.now(), 401, "UNAUTHORIZED", "Authentication is required or the access token is invalid", Map.of()));
                }).accessDeniedHandler((request, response, cause) -> {
                    response.setStatus(403); response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                    mapper.writeValue(response.getOutputStream(), new ApiError(Instant.now(), 403, "FORBIDDEN", "Access to this resource is denied", Map.of()));
                }))
                .addFilterBefore(jwt, UsernamePasswordAuthenticationFilter.class).build();
    }
}
