package com.chriscodecc.fdw_analytics_engine.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import com.chriscodecc.fdw_analytics_engine.dto.RiskEvaluationResponse;
import com.chriscodecc.fdw_analytics_engine.service.RiskEvaluationService;

import jakarta.persistence.EntityNotFoundException;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller providing endpoints for evaluating financial risk classifications.
 */
@RestController 
@RequestMapping("/api/v1/riskEvaluation")
public class RiskEvaluationController {
    
    private final RiskEvaluationService riskEvaluationService;

    public RiskEvaluationController(RiskEvaluationService riskEvaluationService){
        this.riskEvaluationService = riskEvaluationService;
    }

    /**
     * Retrieves the consolidated risk evaluation for a specified company.
     *
     * @param companySymbol the ticker symbol identifying the company (e.g., "AAPL", "SAP")
     * @param period the analysis window in days (defaults to 30)
     * @return a {@link ResponseEntity} containing the {@link RiskEvaluationResponse} if found,
     *         or a 404 Not Found status
     * @throws EntityNotFoundException if the company symbol cannot be resolved
     */
    @GetMapping("/risklevel")
    public ResponseEntity<RiskEvaluationResponse> getRiskLevel(@RequestParam String companySymbol, @RequestParam(required = false, defaultValue = "30") Integer period) throws EntityNotFoundException{
        RiskEvaluationResponse riskEvaluationResponse = riskEvaluationService.culateOverAllRiskLevel(companySymbol);
        if(riskEvaluationResponse == null){
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(riskEvaluationResponse);
    }
    
}
