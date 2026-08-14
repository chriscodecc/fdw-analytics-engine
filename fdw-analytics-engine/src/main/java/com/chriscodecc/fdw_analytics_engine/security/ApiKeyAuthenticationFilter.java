package com.chriscodecc.fdw_analytics_engine.security;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.stereotype.Component;
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
@Component
public class ApiKeyAuthenticationFilter extends OncePerRequestFilter{
    
    @Value("${app.api.key}")
    private String apiKey;

    
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

        if(requestApiKey != null && requestApiKey.equals(apiKey)){
            UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken("ApiKeyUser", null, Collections.emptyList());
            SecurityContextHolder.getContext().setAuthentication(auth);
        }
        filterChain.doFilter(request, response);
    }
}
