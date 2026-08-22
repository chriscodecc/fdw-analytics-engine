package com.chriscodecc.fdw_analytics_engine.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.chriscodecc.fdw_analytics_engine.dto.RiskEvaluationResponse;
import com.chriscodecc.fdw_analytics_engine.dto.RollingMetricDTO;
import com.chriscodecc.fdw_analytics_engine.model.DimCompany;
import com.chriscodecc.fdw_analytics_engine.model.RiskLevel;
import com.chriscodecc.fdw_analytics_engine.repository.DimCompanyRepository;
import com.chriscodecc.fdw_analytics_engine.repository.FactPricesRepository;

import jakarta.persistence.EntityNotFoundException;

@Service
public class RiskEvaluationService {
    private final FactPricesRepository factPricesRepository;
    private final DimCompanyRepository dimCompanyRepository;
    private final AnalyticsService analyticsService;
    private RiskEvaluationResponse riskEvaluationResponse;

    public RiskEvaluationService(FactPricesRepository factPricesRepository, DimCompanyRepository dimCompanyRepository){
        this.factPricesRepository = factPricesRepository;
        this.dimCompanyRepository = dimCompanyRepository;    
        analyticsService = new AnalyticsService(factPricesRepository, dimCompanyRepository);
    
    }

    public RiskEvaluationResponse culateOverAllRiskLevel(String companySymbol, LocalDate today){
        DimCompany company = dimCompanyRepository.findBySymbol(companySymbol).orElseThrow(() -> new EntityNotFoundException("Company not Found!"));
        
        List<RollingMetricDTO> avgList = analyticsService.findRollingMetricsByCompanyIdAndDateRange(
            company.getId(), today.minusDays(30), today);
                
        if (avgList.isEmpty()) {
            throw new IllegalArgumentException("No rolling metric data available for company: " + companySymbol);
        }

        BigDecimal rollingAvg = avgList.get(avgList.size() - 1).getRollingAvg30();
        BigDecimal dailyReturn = analyticsService.dailyReturn(companySymbol, today);
        BigDecimal volumeSpike = analyticsService.calculateAvgVolumeSpike(companySymbol, today);
        BigDecimal sma = analyticsService.calculateSma(company, today);

        // 2. Map metrics to RiskLevels & calculate overall risk
        RiskLevel overallRisk = evaluateRiskScore(dailyReturn, sma, volumeSpike, rollingAvg);

        // 3. Assemble and return the DTO
        RiskEvaluationResponse response = new RiskEvaluationResponse();
        response.setCompanyId(company.getId());
        response.setName(company.getName());
        response.setEvaluatedAt(today);
        response.setRollingAvg(rollingAvg);
        response.setDailyReturn(dailyReturn);
        response.setVolumeSpike(volumeSpike);
        response.setSma(sma);
        response.setOverallRiskLevel(overallRisk);

        return response;
    }

    private RiskLevel evaluateRiskScore(BigDecimal dailyReturn, BigDecimal sma, BigDecimal volumeSpike,
            BigDecimal rollingAvg) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'evaluateRiskScore'");
    }

    public RiskEvaluationResponse culateOverAllRiskLevel(String companySymbol){
        return culateOverAllRiskLevel(companySymbol, LocalDate.now());
    }

    private RiskLevel calculateRollingAvgRiskLevel(){
        return null;
    }

    private RiskLevel calculateDailyReturnRiskLevel(){
        return null;
    }

    private RiskLevel calculateVolumeSpikeRiskLevel(){
        return null;
    }

    private RiskLevel calculateSmaRiskLevel(){
        return null;
    }
}
