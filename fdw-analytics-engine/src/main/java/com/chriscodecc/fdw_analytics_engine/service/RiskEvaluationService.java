package com.chriscodecc.fdw_analytics_engine.service;

import java.time.LocalDate;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.chriscodecc.fdw_analytics_engine.dto.RiskEvaluationResponse;
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
    @Autowired private RiskEvaluationResponse riskEvaluationResponse;

    public RiskEvaluationService(FactPricesRepository factPricesRepository, DimCompanyRepository dimCompanyRepository){
        this.factPricesRepository = factPricesRepository;
        this.dimCompanyRepository = dimCompanyRepository;    
        analyticsService = new AnalyticsService(factPricesRepository, dimCompanyRepository);
    
    }

    public RiskEvaluationResponse culateOverAllRiskLevel(String companySymbol, LocalDate today){
        DimCompany company = dimCompanyRepository.findBySymbol(companySymbol).orElseThrow(() -> new EntityNotFoundException("Company not Found!"));
        riskEvaluationResponse.setCompanyId(company.getId());
        riskEvaluationResponse.setName(company.getName());
        riskEvaluationResponse.setEvaluatedAt(today);
        riskEvaluationResponse.setRollingAvg(analyticsService.findRollingMetricsByCompanyIdAndDateRange(company.getId())
                                                                .get(0)
                                                                .getRollingAvg30());
            
        riskEvaluationResponse.setDailyReturn(analyticsService.dailyReturn(companySymbol));
        //riskEvaluationResponse.setVolumeSpike(analyticsService.v);

        return null;
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
