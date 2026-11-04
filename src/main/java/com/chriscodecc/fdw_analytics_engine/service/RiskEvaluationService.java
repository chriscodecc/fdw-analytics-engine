package com.chriscodecc.fdw_analytics_engine.service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;

import com.chriscodecc.fdw_analytics_engine.Exceptions.CompanyNotFoundException;
import com.chriscodecc.fdw_analytics_engine.dto.RiskEvaluationResponse;
import com.chriscodecc.fdw_analytics_engine.dto.RollingMetricDTO;
import com.chriscodecc.fdw_analytics_engine.model.DimCompany;
import com.chriscodecc.fdw_analytics_engine.model.RawMarketMetric;
import com.chriscodecc.fdw_analytics_engine.model.RiskLevel;
import com.chriscodecc.fdw_analytics_engine.model.RiskThresholds;
import com.chriscodecc.fdw_analytics_engine.repository.DimCompanyRepository;

@Service
public class RiskEvaluationService {
    private final DimCompanyRepository dimCompanyRepository;
    private final AnalyticsService analyticsService;

    // SMA & Return Threshold
    private static final BigDecimal RETURN_THRESHOLD_NORMAL = new BigDecimal("0.02");    
    private static final BigDecimal RETURN_THRESHOLD_HIGH = new BigDecimal("0.05");   
    private static final BigDecimal RETURN_THRESHOLD_CRITICAL = new BigDecimal("0.10"); 
    private static final RiskThresholds smaRiskThreshold = new RiskThresholds(RETURN_THRESHOLD_CRITICAL, RETURN_THRESHOLD_HIGH, RETURN_THRESHOLD_NORMAL);

    // RolligAVG Threshold
    private static final BigDecimal ROLLING_THRESHOLD_NORMAL = new BigDecimal("0.03");     
    private static final BigDecimal ROLLING_THRESHOLD_HIGH = new BigDecimal("0.07");  
    private static final BigDecimal ROLLING_THRESHOLD_CRITICAL = new BigDecimal("0.15"); 
    private static final RiskThresholds rollingRiskThreshold = new RiskThresholds(ROLLING_THRESHOLD_CRITICAL, ROLLING_THRESHOLD_HIGH, ROLLING_THRESHOLD_NORMAL);

    // Volume Spike Multiplikatoren
    private static final BigDecimal VOL_THRESHOLD_NORMAL = new BigDecimal("1.20");
    private static final BigDecimal VOL_THRESHOLD_HIGH = new BigDecimal("1.50");
    private static final BigDecimal VOL_THRESHOLD_CRITICAL = new BigDecimal("2.00");
    private static final RiskThresholds volumeRiskThreshold = new RiskThresholds(VOL_THRESHOLD_CRITICAL, VOL_THRESHOLD_HIGH, VOL_THRESHOLD_NORMAL);

    private final Clock clock;

    public RiskEvaluationService(DimCompanyRepository dimCompanyRepository, AnalyticsService analyticsService, Clock clock){
        this.dimCompanyRepository = dimCompanyRepository;    
        this.analyticsService = analyticsService;
        this.clock = clock;
    }

    /**
     * Calculates the overall risk level for a specified company over a given time range.
     *
     * @param companySymbol the unique ticker symbol of the company (e.g., "SAP", "AAPL")
     * @param startDate the reference date from which the historical calculation starts
     * @param period the duration in days for the analysis window
     * @return the {@link RiskEvaluationResponse} containing the consolidated risk assessment
     * 
     * @throws CompanyNotFoundException if no company matching {@code companySymbol} exists in the database
     * @throws IllegalArgumentException if no rolling metric data is available (typically caused by an invalid or future date)
     */
    public RiskEvaluationResponse culateOverAllRiskLevel(String companySymbol, LocalDate starDate, int period){
        RawMarketMetric metrics;
        DimCompany company = dimCompanyRepository.findBySymbol(companySymbol).orElseThrow(() -> new CompanyNotFoundException("Company not found: " + companySymbol));
        List<RollingMetricDTO> avgList = analyticsService.findRollingMetricsByCompanyIdAndDateRange(
            company.getSymbol(), starDate, period);
        
        if (avgList.isEmpty()) {
            throw new IllegalArgumentException("No rolling metric data available for company: " + companySymbol);
        }
        BigDecimal rollingAvg = avgList.get(avgList.size() - 1).getRollingAvg30();
        BigDecimal dailyReturn = analyticsService.dailyReturn(companySymbol);
        BigDecimal volumeSpike = analyticsService.calculateAvgVolumeSpike(companySymbol);
        BigDecimal sma = analyticsService.getSMA(companySymbol);

        metrics = new RawMarketMetric(avgList.get(0).getClosePrice(), sma, rollingAvg, dailyReturn, volumeSpike);

        // 2. Assemble and return the DTO
        RiskEvaluationResponse response = new RiskEvaluationResponse();
        response.setCompanyId(company.getId());
        response.setName(company.getName());
        response.setEvaluatedAt(starDate);

        // 3. Map metrics to RiskLevels & calculate overall risk
        response = evaluateRiskScore(response, metrics);
        response = calculateOverallRisk(response);

        return response;
    }

    public RiskEvaluationResponse culateOverAllRiskLevel(String companySymbol){
        LocalDate startDate = LocalDate.now(clock);
        return  culateOverAllRiskLevel(companySymbol, startDate, 30);
    }

    /**
     * Evaluates raw market metrics, calculates relative indicator deviations, and sets
     * the respective risk classifications on the given response object.
     *
     * @param response the {@link RiskEvaluationResponse} to be populated with calculated values and risk levels
     * @param metrics the {@link RawMarketMetric} container holding the baseline indicators and closing prices
     * @return the populated {@link RiskEvaluationResponse} containing calculated metrics and assigned risk classifications
     */
    private RiskEvaluationResponse  evaluateRiskScore(RiskEvaluationResponse response, RawMarketMetric metrics) {
        response.setRollingAvg(analyticsService.calculateRelativeDeviation(metrics.todaysClosingPrice(), metrics.rollingAvg()));
        response.setDailyReturn(metrics.dailyReturn());
        response.setVolumeSpike(metrics.volumeSpike());
        response.setSma(analyticsService.calculateRelativeDeviation(metrics.todaysClosingPrice(), metrics.sma()));

        response.setDailyReturnRiskLevel(classifyRisk(response.getDailyReturn(), smaRiskThreshold));
        response.setSmaRiskLevel(classifyRisk(response.getSma(), smaRiskThreshold));
        response.setRollingAvgRiskLevel(classifyRisk(response.getRollingAvg(), rollingRiskThreshold));
        response.setVolumeSpikeRiskLevel(classifyRisk(response.getVolumeSpike(), volumeRiskThreshold));
        
        return response; 
    }

    /**
     * Classifies a numerical indicator value into a {@link RiskLevel} based on defined threshold boundaries.
     * <p>
     * The absolute value of the input is evaluated against descending threshold tiers:
     * {@code CRITICAL}, {@code HIGH}, {@code NORMAL}, falling back to {@code LOW}.
     *
     * @param value the metric value to evaluate (evaluated by its absolute value)
     * @param thresholds the {@link RiskThresholds} containing the limit boundaries for each risk tier
     * @return the determined {@link RiskLevel}
     * 
     * @throws IllegalArgumentException if {@code value} is {@code null}
     */
    public RiskLevel classifyRisk(BigDecimal value, RiskThresholds thresholds){
        if(value == null) {
            throw new IllegalArgumentException("Value is NULL.");
        }
        value = value.abs();

        if (value.compareTo(thresholds.critical()) >= 0) {       
            return RiskLevel.CRITICAL;
        } else if (value.compareTo(thresholds.high()) >= 0) {    
            return RiskLevel.HIGH;
        } else if (value.compareTo(thresholds.normal()) >= 0) {    
            return RiskLevel.NORMAL;
        } else {                                                          
            return RiskLevel.LOW;
        }
    }

    /**
     * Aggregates individual metric risk levels into a weighted overall score and identifies primary drivers.
     * 
     * Evaluates individual risk drivers sorted by score and weight. If any metric reaches
     * {@link RiskLevel#CRITICAL}, the composite score immediately saturates at maximum risk.
     * Drivers with a severity of {@link RiskLevel#HIGH} or greater are tracked as active drivers,
     * with the highest-ranked entry designated as the primary risk driver.
     *
     * @param response the {@link RiskEvaluationResponse} containing individual indicator risk levels to aggregate
     * @return the populated {@link RiskEvaluationResponse} updated with composite risk level and identified risk drivers
     */
    private RiskEvaluationResponse calculateOverallRisk(RiskEvaluationResponse response){
        response.setPrimaryRiskDriver(null);

        List<CalculatedRisk> riskDrivers = new ArrayList<>();
        riskDrivers.add(new CalculatedRisk("dailyReturn", response.getDailyReturnRiskLevel(), 4));
        riskDrivers.add(new CalculatedRisk("sma", response.getSmaRiskLevel(), 3));
        riskDrivers.add(new CalculatedRisk("volumeSpike", response.getVolumeSpikeRiskLevel(), 2));
        riskDrivers.add(new CalculatedRisk("rollingAvg", response.getRollingAvgRiskLevel(), 1));

        riskDrivers.sort(
            Comparator.comparingInt(CalculatedRisk::getScore)
            .thenComparing(CalculatedRisk::weight)
            .reversed()
        );

        List<String> primaryDrivers = new ArrayList<>();
        double overalRiskScore = 0;
        for (CalculatedRisk calculatedRisk : riskDrivers) {
            if(calculatedRisk.level.getScore() == RiskLevel.CRITICAL.getScore() && overalRiskScore < 4){
                overalRiskScore = 4;
            } else if (overalRiskScore < 4) {
                double multiply =  (double) calculatedRisk.weight()/10;
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

    public record CalculatedRisk(String name, RiskLevel level, int weight) {

        public int getScore(){
            return level.getScore();
        }

        @Override
        public final String toString() {
            // TODO Auto-generated method stub
            return "Name: " + name + " RiskLevel: " + level.name() + " " + level.getScore() + " weigth: " + weight();
        }
    }
}
