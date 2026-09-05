package com.chriscodecc.fdw_analytics_engine.service;

import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.chriscodecc.fdw_analytics_engine.Exceptions.CompanyNotFoundException;
import com.chriscodecc.fdw_analytics_engine.dto.RiskEvaluationResponse;
import com.chriscodecc.fdw_analytics_engine.dto.RollingMetricDTO;
import com.chriscodecc.fdw_analytics_engine.model.DimCompany;
import com.chriscodecc.fdw_analytics_engine.model.RiskLevel;
import com.chriscodecc.fdw_analytics_engine.repository.DimCompanyRepository;
import com.chriscodecc.fdw_analytics_engine.repository.FactPricesRepository;

import io.micrometer.common.util.internal.logging.InternalLogLevel;
import jakarta.persistence.EntityNotFoundException;
import lombok.val;

@Service
public class RiskEvaluationService {
    private final FactPricesRepository factPricesRepository;
    private final DimCompanyRepository dimCompanyRepository;
    private final AnalyticsService analyticsService;
    private RiskEvaluationResponse riskEvaluationResponse;

    // SMA & Return Threshold
    private static final BigDecimal RETURN_THRESHOLD_NORMAL = new BigDecimal("0.02");      // 2%
    private static final BigDecimal RETURN_THRESHOLD_HIGH = new BigDecimal("0.05");   // 5%
    private static final BigDecimal RETURN_THRESHOLD_CRITICAL = new BigDecimal("0.10"); // 10% (dein bisheriger SMA_THRESHOLD)

    // RolligAVG Threshold
    private static final BigDecimal ROLLING_THRESHOLD_NORMAL = new BigDecimal("0.03");      // 2%
    private static final BigDecimal ROLLING_THRESHOLD_HIGH = new BigDecimal("0.07");   // 7%
    private static final BigDecimal ROLLING_THRESHOLD_CRITICAL = new BigDecimal("0.15"); // 10% (dein bisheriger SMA_THRESHOLD)

    // Volume Spike Multiplikatoren
    private static final BigDecimal VOL_THRESHOLD_NORMAL = new BigDecimal("1.20");
    private static final BigDecimal VOL_THRESHOLD_HIGH = new BigDecimal("1.50");
    private static final BigDecimal VOL_THRESHOLD_CRITICAL = new BigDecimal("2.00");

    public RiskEvaluationService(FactPricesRepository factPricesRepository, DimCompanyRepository dimCompanyRepository, AnalyticsService analyticsService){
        this.factPricesRepository = factPricesRepository;
        this.dimCompanyRepository = dimCompanyRepository;    
        this.analyticsService = analyticsService;
    
    }

    public RiskEvaluationResponse culateOverAllRiskLevel(String companySymbol, LocalDate today){
        DimCompany company = dimCompanyRepository.findBySymbol(companySymbol).orElseThrow(() -> new CompanyNotFoundException("Company not found: " + companySymbol));
        
        List<RollingMetricDTO> avgList = analyticsService.findRollingMetricsByCompanyIdAndDateRange(
            company.getSymbol(), today.minusDays(30), today);
                
        if (avgList.isEmpty()) {
            throw new IllegalArgumentException("No rolling metric data available for company: " + companySymbol);
        }

        BigDecimal rollingAvg = avgList.get(avgList.size() - 1).getRollingAvg30();
        BigDecimal dailyReturn = analyticsService.dailyReturn(companySymbol, today);
        BigDecimal volumeSpike = analyticsService.calculateAvgVolumeSpike(companySymbol, today);
        BigDecimal sma = analyticsService.getSMA(companySymbol, today);

        // 2. Assemble and return the DTO
        RiskEvaluationResponse response = new RiskEvaluationResponse();
        response.setCompanyId(company.getId());
        response.setName(company.getName());
        response.setEvaluatedAt(today);

        // 3. Map metrics to RiskLevels & calculate overall risk
        response = evaluateRiskScore(response, rollingAvg, dailyReturn, volumeSpike, sma, avgList.get(0).getClosePrice());
        response = calculateOverallRisk(response);

        return response;
    }

    public RiskEvaluationResponse culateOverAllRiskLevel(String companySymbol){
        LocalDate today = LocalDate.now();
        return culateOverAllRiskLevel(companySymbol, today);
    }

    private RiskEvaluationResponse evaluateRiskScore(RiskEvaluationResponse response, BigDecimal rollingAvg, BigDecimal dailyReturn, BigDecimal volumeSpike, BigDecimal sma, BigDecimal todaysClosingPrice) {
        response.setRollingAvg(analyticsService.calculateRelativeDeviation(todaysClosingPrice, rollingAvg));
        response.setDailyReturn(dailyReturn);
        response.setVolumeSpike(volumeSpike);
        response.setSma(analyticsService.calculateRelativeDeviation(todaysClosingPrice, sma));

        response.setDailyReturnRiskLevel(classifyRisk(response.getDailyReturn(), RETURN_THRESHOLD_CRITICAL, RETURN_THRESHOLD_HIGH, RETURN_THRESHOLD_NORMAL));
        response.setSmaRiskLevel(classifyRisk(response.getSma(), RETURN_THRESHOLD_CRITICAL, RETURN_THRESHOLD_HIGH, RETURN_THRESHOLD_NORMAL));
        response.setRollingAvgRiskLevel(classifyRisk(response.getRollingAvg(), ROLLING_THRESHOLD_CRITICAL, ROLLING_THRESHOLD_HIGH, ROLLING_THRESHOLD_NORMAL));
        response.setVolumeSpikeRiskLevel(classifyRisk(response.getVolumeSpike(), VOL_THRESHOLD_CRITICAL, VOL_THRESHOLD_HIGH, VOL_THRESHOLD_NORMAL));
        
        return response; 
    }

    private RiskLevel classifyRisk(BigDecimal value, BigDecimal critical, BigDecimal high, BigDecimal normal){
        if(value == null) {
            throw new IllegalArgumentException("Value is NULL.");
        }
        value = value.abs();

        if (value.compareTo(critical) >= 0) {       
            return RiskLevel.CRITICAL;
        } else if (value.compareTo(high) >= 0) {    
            return RiskLevel.HIGH;
        } else if (value.compareTo(normal) >= 0) {    
            return RiskLevel.NORMAL;
        } else {                                                          
            return RiskLevel.LOW;
        }
    }

    private RiskEvaluationResponse calculateOverallRisk(RiskEvaluationResponse response){
        response.setPrimaryRiskDriver(null);

        List<CalculatedRisk> riskDrivers = new ArrayList<>();
        riskDrivers.add(new CalculatedRisk("dailyReturn", response.getDailyReturnRiskLevel(), 4));
        riskDrivers.add(new CalculatedRisk("sma", response.getSmaRiskLevel(), 3));
        riskDrivers.add(new CalculatedRisk("volumeSpike", response.getVolumeSpikeRiskLevel(), 2));
        riskDrivers.add(new CalculatedRisk("rollingAvg", response.getRollingAvgRiskLevel(), 1));

        riskDrivers.sort(
            Comparator.comparingInt(CalculatedRisk::getScore)
            .thenComparing(CalculatedRisk::weigth)
            .reversed()
        );

        List<String> primaryDrivers = new ArrayList<>();
        double overalRiskScore = 0;
        for (CalculatedRisk calculatedRisk : riskDrivers) {
            if(calculatedRisk.level.getScore() == RiskLevel.CRITICAL.getScore() && overalRiskScore < 4){
                overalRiskScore = 4;
            } else if (overalRiskScore < 4) {
                double multiply =  (double) calculatedRisk.weigth()/10;
                overalRiskScore += (calculatedRisk.level.getScore() * multiply);
            }
            if(calculatedRisk.getScore() >= RiskLevel.HIGH.getScore()) {
                primaryDrivers.add(calculatedRisk.name());
            } 
        }

        if(!primaryDrivers.isEmpty()){
            response.setActiveRiskDrivers(primaryDrivers);
            response.setPrimaryRiskDriver(primaryDrivers.get(0));
        }

        int finalScore = (int) Math.round(overalRiskScore);
        response.setOverallRiskLevel(RiskLevel.fromScore(finalScore));
        
        return response;
    }

    public record CalculatedRisk(String name, RiskLevel level, int weigth) {

        public int getScore(){
            return level.getScore();
        }

        @Override
        public final String toString() {
            // TODO Auto-generated method stub
            return "Name: " + name + " RiskLevel: " + level.name() + " " + level.getScore() + " weigth: " + weigth;
        }
    }
}
