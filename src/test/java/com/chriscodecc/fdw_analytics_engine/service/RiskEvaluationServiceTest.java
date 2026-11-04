package com.chriscodecc.fdw_analytics_engine.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.annotation.Import;

import com.chriscodecc.fdw_analytics_engine.Exceptions.CompanyNotFoundException;
import com.chriscodecc.fdw_analytics_engine.config.ClockConfig;
import com.chriscodecc.fdw_analytics_engine.dto.RiskEvaluationResponse;
import com.chriscodecc.fdw_analytics_engine.dto.RollingMetricDTO;
import com.chriscodecc.fdw_analytics_engine.model.DimCompany;
import com.chriscodecc.fdw_analytics_engine.model.DimDate;
import com.chriscodecc.fdw_analytics_engine.model.RiskLevel;
import com.chriscodecc.fdw_analytics_engine.model.RiskThresholds;
import com.chriscodecc.fdw_analytics_engine.repository.DimCompanyRepository;

//  mvn test -Dtest=RiskEvaluationServiceTest

@ExtendWith(MockitoExtension.class)
@Import(ClockConfig.class)
public class RiskEvaluationServiceTest {
    @Spy
    private Clock clock = Clock.fixed(
        Instant.parse("2026-07-31T00:00:00Z"), 
        ZoneId.of("UTC")
    );


    @Mock
    private AnalyticsService analyticsService;
    @Mock
    private DimCompanyRepository dimCompanyRepository;
    @InjectMocks 
    RiskEvaluationService riskEvaluationService;

    private DimCompany dimCompany;
    private List<RollingMetricDTO> avgList;
    private DimDate date;

    int days;
    Integer companyId;
    String symbol;
    String companyName;

    LocalDate todayDate;

    BigDecimal dailyReturn;
    BigDecimal sma;
    BigDecimal volumeSpike;

    RiskEvaluationResponse expectedResult = new RiskEvaluationResponse();


    // TESTDATA FOR CRITICAL 
    private BigDecimal criticalDailyReturn;
    private BigDecimal criticalVolumeSpike;
    private BigDecimal highSmaPrice;
    private BigDecimal mockSmaHighDev;
    private RiskEvaluationResponse expectedCriticalResult = new RiskEvaluationResponse();
    private List<RollingMetricDTO> avgListCritical;

    

    @BeforeEach
    void setUp(){ 
        companyName = "Apple Inc.";
        symbol = "AAPL";
        days = 7;
        companyId = 1;
        todayDate = LocalDate.now(clock);

        dimCompany = new DimCompany();
        dimCompany.setId(companyId);
        dimCompany.setName(companyName);
        dimCompany.setSymbol(symbol);
        dimCompany.setCountry("USA");
        dimCompany.setIndustry("Technology");

        date = new DimDate();
        date.setDayToday((short) todayDate.getDayOfMonth());
        date.setMonthToday((short) todayDate.getMonthValue());
        date.setYearToday((short) todayDate.getYear());

        avgList = new ArrayList<>();
        avgList.add(new RollingMetricDTO(companyId, companyName, todayDate.minusDays(5), new BigDecimal("26299.740234"), new BigDecimal("24826.419856800000")));
        avgList.add(new RollingMetricDTO(companyId, companyName, todayDate.minusDays(4), new BigDecimal("26440.310547"), new BigDecimal("24903.203190133333")));
        avgList.add(new RollingMetricDTO(companyId, companyName, todayDate.minusDays(3), new BigDecimal("26128.359375"), new BigDecimal("24958.939843766667")));
        avgList.add(new RollingMetricDTO(companyId, companyName, todayDate.minusDays(2), new BigDecimal("25983.039063"), new BigDecimal("25026.688802100000")));
        avgList.add(new RollingMetricDTO(companyId, companyName, todayDate.minusDays(1), new BigDecimal("26367.240234"), new BigDecimal("25095.332812500000")));
        avgList.add(new RollingMetricDTO(companyId, companyName, todayDate, new BigDecimal("26569.990234"), new BigDecimal("225167.644140600000")));

        dailyReturn = new BigDecimal("0.0077");
        volumeSpike = new BigDecimal("0.00");
        sma = new BigDecimal("26468.615234");

        expectedResult.setCompanyId(companyId);
        expectedResult.setName(companyName);
        expectedResult.setPriceDate(null);
        expectedResult.setEvaluatedAt(todayDate);

        expectedResult.setRollingAvg(new BigDecimal("0.04498220362126475449391192589008265"));
        expectedResult.setDailyReturn(new BigDecimal("0.0077"));
        expectedResult.setVolumeSpike(new BigDecimal("0.00"));
        expectedResult.setSma(new BigDecimal("0.006380197774119791339893259487320258"));

        expectedResult.setRollingAvgRiskLevel(RiskLevel.NORMAL);
        expectedResult.setDailyReturnRiskLevel(RiskLevel.LOW);
        expectedResult.setVolumeSpikeRiskLevel(RiskLevel.LOW);
        expectedResult.setSmaRiskLevel(RiskLevel.LOW);

        expectedResult.setOverallRiskLevel(RiskLevel.LOW);
        expectedResult.setPrimaryRiskDriver(null);
        expectedResult.setActiveRiskDrivers(null);


        // Critical Test Data Values
        criticalDailyReturn = new BigDecimal("-0.0850");
        criticalVolumeSpike = new BigDecimal("4.50");
        highSmaPrice = new BigDecimal("29800.500000");
        mockSmaHighDev = new BigDecimal("0.13310000000000000000000000000000000");

        expectedCriticalResult.setCompanyId(companyId);
        expectedCriticalResult.setName(companyName);
        expectedCriticalResult.setPriceDate(null);
        expectedCriticalResult.setEvaluatedAt(todayDate);

        expectedCriticalResult.setRollingAvg(new BigDecimal("0.04498220362126475449391192589008265"));
        expectedCriticalResult.setDailyReturn(criticalDailyReturn);
        expectedCriticalResult.setVolumeSpike(criticalVolumeSpike);
        expectedCriticalResult.setSma(mockSmaHighDev);

        expectedCriticalResult.setRollingAvgRiskLevel(RiskLevel.NORMAL);
        expectedCriticalResult.setDailyReturnRiskLevel(RiskLevel.CRITICAL);
        expectedCriticalResult.setVolumeSpikeRiskLevel(RiskLevel.CRITICAL);
        expectedCriticalResult.setSmaRiskLevel(RiskLevel.HIGH);

        expectedCriticalResult.setOverallRiskLevel(RiskLevel.CRITICAL);
        expectedCriticalResult.setPrimaryRiskDriver("sma");
        expectedCriticalResult.setActiveRiskDrivers(List.of("dailyReturn", "volumeSpike", "sma"));

        avgListCritical = new ArrayList<>();
        avgListCritical.add(new RollingMetricDTO(companyId, companyName, todayDate.minusDays(5), new BigDecimal("26299.740234"), new BigDecimal("24826.419856800000")));
        avgListCritical.add(new RollingMetricDTO(companyId, companyName, todayDate.minusDays(4), new BigDecimal("26440.310547"), new BigDecimal("24903.203190133333")));
        avgListCritical.add(new RollingMetricDTO(companyId, companyName, todayDate.minusDays(3), new BigDecimal("26128.359375"), new BigDecimal("24958.939843766667")));
        avgListCritical.add(new RollingMetricDTO(companyId, companyName, todayDate.minusDays(2), new BigDecimal("25983.039063"), new BigDecimal("25026.688802100000")));
        avgListCritical.add(new RollingMetricDTO(companyId, companyName, todayDate.minusDays(1), new BigDecimal("26367.240234"), new BigDecimal("25095.332812500000")));
        avgListCritical.add(new RollingMetricDTO(companyId, companyName, todayDate, new BigDecimal("26569.990234"), new BigDecimal("225167.644140600000")));
    }

    @Test
    void testCalculateOverallRiskLevel(){
        when(dimCompanyRepository.findBySymbol(symbol)).thenReturn(Optional.of(dimCompany));
        when(analyticsService.findRollingMetricsByCompanyIdAndDateRange(symbol, todayDate, 30)).thenReturn(avgList);
        when(analyticsService.dailyReturn(symbol)).thenReturn(dailyReturn);
        when(analyticsService.calculateAvgVolumeSpike(symbol)).thenReturn(volumeSpike);
        when(analyticsService.getSMA(symbol)).thenReturn(sma);
        
        when(analyticsService.calculateRelativeDeviation(avgList.get(0).getClosePrice(),  avgList.get(avgList.size() - 1)
                                        .getRollingAvg30())).thenReturn(new BigDecimal("0.04498220362126475449391192589008265"));

        when(analyticsService.calculateRelativeDeviation(avgList.get(0).getClosePrice(), sma))
                                        .thenReturn(new BigDecimal("0.006380197774119791339893259487320258"));

        RiskEvaluationResponse response  = riskEvaluationService.culateOverAllRiskLevel(symbol);
        
        assertEquals(expectedResult.getDailyReturn(), response.getDailyReturn());
        assertEquals(expectedResult.getPrimaryRiskDriver(), response.getPrimaryRiskDriver());
    }

    @Test 
    void testCalculateOverallRiskLevel_WITH_CRITICAL_DATA_shouldReturnAPrimaryRiskDriver(){
        when(dimCompanyRepository.findBySymbol(symbol)).thenReturn(Optional.of(dimCompany));
        when(analyticsService.findRollingMetricsByCompanyIdAndDateRange(symbol, todayDate, 30)).thenReturn(avgListCritical);
        when(analyticsService.dailyReturn(symbol)).thenReturn(criticalDailyReturn);
        when(analyticsService.calculateAvgVolumeSpike(symbol)).thenReturn(criticalVolumeSpike);
        when(analyticsService.getSMA(symbol)).thenReturn(highSmaPrice);

        when(analyticsService.calculateRelativeDeviation(avgListCritical.get(0).getClosePrice(),  avgListCritical.get(avgListCritical.size() - 1).getRollingAvg30())).thenReturn(new BigDecimal("0.069"));
        when(analyticsService.calculateRelativeDeviation(avgListCritical.get(0).getClosePrice(), highSmaPrice)).thenReturn(new BigDecimal("0.1174731889062264055972215231288066"));

        RiskEvaluationResponse response  = riskEvaluationService.culateOverAllRiskLevel(symbol);

        assertEquals(expectedCriticalResult.getPrimaryRiskDriver(), response.getPrimaryRiskDriver());
        assertEquals(expectedCriticalResult.getActiveRiskDrivers().get(1), response.getActiveRiskDrivers().get(1));
        assertEquals(3, response.getActiveRiskDrivers().size());
    }

    @Test
    void testEmpyt_avgList_shouldReturn_IllegalArgumentException(){
        when(dimCompanyRepository.findBySymbol(symbol)).thenReturn(Optional.of(dimCompany));
        when(analyticsService.findRollingMetricsByCompanyIdAndDateRange(symbol, todayDate, 30)).thenReturn(new ArrayList<>());
    
         assertThrows(IllegalArgumentException.class, () -> {
            riskEvaluationService.culateOverAllRiskLevel(symbol);
        });
    }

    //@Test
    void testCompanyNotFound_shouldThrowEntityNotFoundException() {
        doReturn(Optional.empty()).when(dimCompanyRepository.findBySymbol(symbol));

        assertThrows(CompanyNotFoundException.class, () -> {
            riskEvaluationService.culateOverAllRiskLevel("TEST");
        });
    }

    @ParameterizedTest(name = "Active metric {0} should map to {1}")
    @CsvSource({
        "0.0199, LOW",     // Below boundary
        "0.02,   NORMAL",  // Exact lower boundary for NORMAL
        "0.05,   HIGH",    // Exact lower boundary for HIGH
        "0.10,   CRITICAL" // Exact lower boundary for CRITICAL
    })
    void testActiveThresholdCalculations(BigDecimal value, RiskLevel expected){
        RiskThresholds riskThresholds = new RiskThresholds(new BigDecimal("0.10"), new BigDecimal("0.05"), new BigDecimal("0.02") );
        RiskLevel actual = riskEvaluationService.classifyRisk(
            value, 
            riskThresholds
        );

        assertEquals(expected, actual);
    }

    @Test
    @DisplayName("Passive input (null) should be rejected immediately")
    void shouldThrowIllegalArgumentExceptionWhenInputIsPassiveOrNull() {
        RiskThresholds riskThresholds = new RiskThresholds(new BigDecimal("0.10"), new BigDecimal("0.05"), new BigDecimal("0.02") );
        assertThrows(IllegalArgumentException.class, () -> 
            riskEvaluationService.classifyRisk(
                null, // Passive / non-existent data
                riskThresholds
            )
        );
    }

    @Test
    void testCalculateOverallRiskLevel_ShouldReturn_LOW(){
        when(dimCompanyRepository.findBySymbol(symbol)).thenReturn(Optional.of(dimCompany));
        when(analyticsService.findRollingMetricsByCompanyIdAndDateRange(symbol, todayDate, 30)).thenReturn(avgList);
        when(analyticsService.dailyReturn(symbol)).thenReturn(new BigDecimal("0.01"));
        when(analyticsService.calculateAvgVolumeSpike(symbol)).thenReturn(new BigDecimal("1.01"));
        when(analyticsService.getSMA(symbol)).thenReturn(new BigDecimal("0.01"));

        doReturn(new BigDecimal("0.01")).when(analyticsService).calculateRelativeDeviation(any(), any());

        RiskEvaluationResponse response  = riskEvaluationService.culateOverAllRiskLevel(symbol);

        assertEquals(null, response.getActiveRiskDrivers());
        assertEquals(null, response.getPrimaryRiskDriver());
    }





}
