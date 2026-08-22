package com.chriscodecc.fdw_analytics_engine.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import com.chriscodecc.fdw_analytics_engine.model.RiskLevel;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RiskEvaluationResponse {
    Integer companyId;
    String name; 
    LocalDate priceDate;
    LocalDate evaluatedAt;

    //Metric
    BigDecimal rollingAvg;
    BigDecimal dailyReturn;
    BigDecimal volumeSpike; 
    BigDecimal sma;

    //// Individual Indicator Levels
    RiskLevel rollingAvgRiskLevel;  // findRollingMetricsByCompanyIdAndDateRange get size()-1 (latest)
    RiskLevel dailyReturnRiskLevel; // dailyReturn()
    RiskLevel volumeSpikeRiskLevel; //calculateAvgVolumeSpike
    RiskLevel smaRiskLevel;         //calculateSma

    // Synthesized Overall Verdict
    RiskLevel overallRiskLevel;
    String primaryRiskDriver;           // The dominant trigger (or null/NONE if clean)
    List<String> activeRiskDrivers;     // All drivers exceeding thresholds
}
