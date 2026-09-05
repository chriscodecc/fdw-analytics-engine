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


@RestController 
@RequestMapping("/api/v1/riskEvaluation")
public class RiskEvaluationController {
    
    private final RiskEvaluationService riskEvaluationService;

    public RiskEvaluationController(RiskEvaluationService riskEvaluationService){
        this.riskEvaluationService = riskEvaluationService;
    }

    @GetMapping("/risklevel")
    public ResponseEntity<RiskEvaluationResponse> getRiskLevel(@RequestParam String companySymbol) throws EntityNotFoundException{
        RiskEvaluationResponse riskEvaluationResponse = riskEvaluationService.culateOverAllRiskLevel(companySymbol);
        if(riskEvaluationResponse == null){
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(riskEvaluationResponse);
    }
    
}
