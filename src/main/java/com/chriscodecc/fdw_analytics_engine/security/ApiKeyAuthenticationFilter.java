package com.chriscodecc.fdw_analytics_engine.security;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Collections;

import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.composite.CompositeMeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.micrometer.prometheusmetrics.PrometheusConfig;
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import io.micrometer.core.instrument.Clock;
import io.micrometer.core.instrument.Counter;

import org.slf4j.Logger;

/**
 * Custom security filter responsible for API Key authentication.
 * Intercepts incoming HTTP requests to validate the presence and correctness 
 * of the required 'API_KEY' header before granting access to secured endpoints.
 */

@Component 
public class ApiKeyAuthenticationFilter extends OncePerRequestFilter{
    
    @Value("${app.api.key}")
    private String configuredApiKey;

    private static final Logger logger = LoggerFactory.getLogger(ApiKeyAuthenticationFilter.class);
    private final Counter missingApiKeyCounter;
    private final Counter invalidApiKeyCounter;
    
    public ApiKeyAuthenticationFilter(MeterRegistry registry) {
        this.missingApiKeyCounter = Counter.builder("security.auth.failures")
                .description("Count of failed API key authentication attempts")
                .tag("reason", "MISSING_API_KEY")
                .register(registry);

        this.invalidApiKeyCounter = Counter.builder("security.auth.failures")
                .description("Count of failed API key authentication attempts")
                .tag("reason", "INVALID_API_KEY")
                .register(registry);
    }



    
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

        if(requestApiKey == null || requestApiKey.isBlank()){
            logger.warn("event=AUTH_FAILURE reason=MISSING_API_KEY method={} uri={} remoteAddr={}",
                    request.getMethod(), request.getRequestURI(), request.getRemoteAddr());
            missingApiKeyCounter.increment();

            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized"); return;
        }
        else if (requestApiKey != null && configuredApiKey != null) {
            boolean isValid = MessageDigest.isEqual(
                    requestApiKey.getBytes(StandardCharsets.UTF_8), 
                    (configuredApiKey.getBytes(StandardCharsets.UTF_8))
            );

            if(isValid){
                UsernamePasswordAuthenticationToken auth = 
                        new UsernamePasswordAuthenticationToken("ApiKeyUser", null, Collections.emptyList());
                SecurityContextHolder.getContext().setAuthentication(auth);
            }
            else{
                logger.warn("event=AUTH_FAILURE reason=INVALID_API_KEY method={} uri={} remoteAddr={}",
                            request.getMethod(), request.getRequestURI(), request.getRemoteAddr());
                invalidApiKeyCounter.increment();
                
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized"); return;
            }
        }

        filterChain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.equals("/actuator/health") || path.equals("/actuator/prometheus");
    }
}
