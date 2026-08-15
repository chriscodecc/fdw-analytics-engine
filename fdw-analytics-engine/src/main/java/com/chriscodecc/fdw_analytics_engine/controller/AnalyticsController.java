package com.chriscodecc.fdw_analytics_engine.controller;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.chriscodecc.fdw_analytics_engine.dto.RollingMetricDTO;
import com.chriscodecc.fdw_analytics_engine.service.AnalyticsService;

import jakarta.persistence.EntityNotFoundException;


@Controller
@RequestMapping("/api/v1/analytics")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService){
        this.analyticsService = analyticsService;
    }
    
    /**
     * REST endpoint to calculate the daily percentage return for a specific company.
     * Uses the two most recent available trading prices.
     *
     * @param symbol the unique ticker symbol of the company (e.g., "DAX")
     * @return ResponseEntity containing the daily return as a decimal ratio (e.g., 0.05 for a 5% gain)
     * @throws EntityNotFoundException (404) if the company symbol is unknown
     */
    @GetMapping("/daily-return")
    public ResponseEntity<BigDecimal> getDailyReturn(@RequestParam String companySymbol) throws EntityNotFoundException{
        BigDecimal result = analyticsService.dailyReturn(companySymbol);
        return ResponseEntity.ok(result);
    }

    /**
     * REST endpoint to check if the daily percentage return exceeds or falls below 
     * the predefined absolute threshold (10%).
     *
     * @param symbol the unique ticker symbol of the company (e.g., "DAX")
     * @return ResponseEntity containing true if the absolute return meets or exceeds the threshold, false otherwise
     * @throws EntityNotFoundException (404) if the company symbol is unknown
     */
    @GetMapping("/threshold-alert")
    public ResponseEntity<Boolean> getThresholdAlert(@RequestParam String companySymbol) {
        return ResponseEntity.ok(analyticsService.checkDailyReturnThreshold(companySymbol));
    }

    /**
     * REST endpoint to check if today's low price of a given company drops below 
     * the 7-day Simple Moving Average (SMA) by more than 10%.
     *
     * @param symbol the unique ticker symbol of the company (e.g., "DAX")
     * @return ResponseEntity containing true if the low price drops below the threshold, false otherwise
     * @throws EntityNotFoundException (404) if the company symbol is unknown
     */
    @GetMapping("/sma")
    public ResponseEntity<Boolean> checkSmaAlert(@RequestParam String companySymbol) {
        return ResponseEntity.ok(analyticsService.simpleMovingAverageAlert(companySymbol));
    }

    /**
     * REST emdpoint to check for unusual volume spikes for a specific compnay
     * 
     * @param symbol the unique ticker symbol (e.g., "DAX")
     * @param date the reference date for the analysis (Format: YYYY-MM-DD)
     * @return ResponseEntity containing true if a spike was detected, false otherwise
     * @throws EntityNotFoundException (404) if the company symbol is unknown
     */ 
    @GetMapping("/volume-spike")
    public ResponseEntity<Boolean> checkVolumeSpike(@RequestParam String companySymbol) {
        return ResponseEntity.ok(analyticsService.volumeSpikeAlert(companySymbol));
    }
    
     @GetMapping("/avg30")
    public ResponseEntity<List<RollingMetricDTO>> rollingMetricAVG() {
        return ResponseEntity.ok(analyticsService.findRollingMetricsByCompanyIdAndDateRange(Long.valueOf(1)));
    }
    
    
}
