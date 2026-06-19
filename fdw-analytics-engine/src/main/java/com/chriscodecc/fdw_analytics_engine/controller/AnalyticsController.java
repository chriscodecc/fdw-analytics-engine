package com.chriscodecc.fdw_analytics_engine.controller;

import java.math.BigDecimal;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.chriscodecc.fdw_analytics_engine.service.AnalyticsService;

import jakarta.persistence.EntityNotFoundException;


@Controller
@RequestMapping("/api/v1/analytics")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService){
        this.analyticsService = analyticsService;
    }
    
    @GetMapping("/daily-return")
    public ResponseEntity<BigDecimal> getDailyReturn(@RequestParam String companySymbol) throws EntityNotFoundException{
        BigDecimal result = analyticsService.dailyReturn(companySymbol);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/threshold-alerts")
    public ResponseEntity<Boolean> getThresholdAlerts(@RequestParam String companySymbol) {
        return ResponseEntity.ok(analyticsService.checkDailyReturnThreshold(companySymbol));
    }

    @GetMapping("/sma")
    public ResponseEntity<Boolean> checkSmaAlert(@RequestParam String companySymbol) {
        return ResponseEntity.ok(analyticsService.simpleMovingAverageAlert(companySymbol));
    }
    
    
}
