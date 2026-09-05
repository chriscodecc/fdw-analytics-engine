package com.chriscodecc.fdw_analytics_engine.security;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Collections;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Custom security filter responsible for API Key authentication.
 * Intercepts incoming HTTP requests to validate the presence and correctness 
 * of the required 'API_KEY' header before granting access to secured endpoints.
 */

public class ApiKeyAuthenticationFilter extends OncePerRequestFilter{
    
    @Value("${app.api.key}")
    private String configuredApiKey;

    
    /**
     * Performs the actual filter logic for incoming requests. Extracts the 'API_KEY' header,
     * compares it against the configured application key, and establishes a stateless 
     * authentication context upon success. Responds with an HTTP 401 Unauthorized status if validation fails.
     * 
     * @param request the incoming HttpServletRequest
     * @param response the outgoing HttpServletResponse
     * @param filterChain the remaining filter chain to execute
     * @throws ServletException if a servlet-specific error occurs during processing
     * @throws IOException if an I/O error occurs during processing
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
  
        String requestApiKey = request.getHeader("API_KEY");

        if (requestApiKey != null && configuredApiKey != null) {
            boolean isValid = MessageDigest.isEqual(
                    requestApiKey.getBytes(StandardCharsets.UTF_8), 
                    (configuredApiKey.getBytes(StandardCharsets.UTF_8))
            );

            if(isValid){
                UsernamePasswordAuthenticationToken auth = 
                        new UsernamePasswordAuthenticationToken("ApiKeyUser", null, Collections.emptyList());
                SecurityContextHolder.getContext().setAuthentication(auth);
            }
        }
        filterChain.doFilter(request, response);
    }
}
