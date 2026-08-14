package com.chriscodecc.fdw_analytics_engine.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.chriscodecc.fdw_analytics_engine.security.ApiKeyAuthenticationFilter;

/**
 * Global Security Configuration for the analytics engine backend.
 * Configures stateless machine-to-machine (M2M) authentication using a custom 
 * API Key filter and enforces secure transport layer requirements.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final ApiKeyAuthenticationFilter apiKeyFilter;

    public SecurityConfig(ApiKeyAuthenticationFilter apiKeyFilter){
        this.apiKeyFilter = apiKeyFilter;
    }

    /**
     * Defines the security filter chain. Registers the custom API Key filter, 
     * disables session-based security features (CSRF/CORS) for a stateless architecture, 
     * and establishes route-based authorization rules.
     * 
     * @param http the HttpSecurity object to build the configuration
     * @return the constructed SecurityFilterChain
     * @throws Exception if an error occurs during the security configuration setup
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .addFilterBefore(apiKeyFilter, UsernamePasswordAuthenticationFilter.class)
            .csrf(csrf -> csrf.disable())
            .cors(cors -> cors.disable())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/error").permitAll()
                .requestMatchers("/api/v1/analytics/daily-return").permitAll()
                .requestMatchers("/api/**").authenticated()
                .anyRequest().authenticated()
            );
            
        return http.build();
    }
   
}
