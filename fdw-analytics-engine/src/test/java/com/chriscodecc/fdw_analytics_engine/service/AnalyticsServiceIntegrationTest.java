package com.chriscodecc.fdw_analytics_engine.service;

import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.jdbc.Sql;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.chriscodecc.fdw_analytics_engine.config.ClockConfig;
import com.chriscodecc.fdw_analytics_engine.dto.RiskEvaluationResponse;
import com.chriscodecc.fdw_analytics_engine.dto.RollingMetricDTO;
import com.chriscodecc.fdw_analytics_engine.model.RiskLevel;
import com.chriscodecc.fdw_analytics_engine.repository.DimCompanyRepository;
import com.chriscodecc.fdw_analytics_engine.repository.FactPricesRepository;
import io.restassured.common.mapper.TypeRef;

import io.restassured.RestAssured;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.time.Clock;

import static io.restassured.RestAssured.given;

//  mvn test -Dtest=AnalyticsServiceIntegrationTest

@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(ClockConfig.class)
public class AnalyticsServiceIntegrationTest {

    @Autowired 
    Clock clock;
    
    @SuppressWarnings({ "resource", "deprecation" })
    @Container
    protected static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test")
            .withInitScript("db/schema.sql")
            .withLogConsumer(new Slf4jLogConsumer(LoggerFactory.getLogger("POSTGRES-CONTAINER")));;

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    
    }
    @LocalServerPort
    private int port;

    @Autowired
    FactPricesRepository factPricesRepository; 
    @Autowired
    AnalyticsService analyticsService;
    @Autowired 
    DimCompanyRepository dimCompanyRepository;
    
    @BeforeEach
    void beforeEach(){
        RestAssured.port = port;
        RestAssured.baseURI = "https://localhost";
        RestAssured.useRelaxedHTTPSValidation();   
    }

    @Test
    @Sql(scripts = "/db/cleanup.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = "/db/insert_risk_current_prices.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void containerIsRunningAndInitialized() {
        assertThat(postgres.isRunning()).isTrue();
        assertThat(factPricesRepository.count()).isGreaterThanOrEqualTo(0);
    }

    @Test
    @Sql(scripts = "/db/cleanup.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = "/db/insert_test_prices.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void findRollingMetricsByCompanyIdAndDateRange_ShouldMapAllFieldsAndCalculateAccurateRollingAverages(){
        Integer companyId = 1;
        String companySymbol = "Nikkei225";
        LocalDate startDate = LocalDate.parse("2026-07-01");
        LocalDate endDate = LocalDate.parse("2026-07-31");

        RollingMetricDTO rmDTO1 = new RollingMetricDTO(companyId, companySymbol, startDate, new BigDecimal("100.00"), new BigDecimal("100.00"));
        RollingMetricDTO rmDTO2 = new RollingMetricDTO(companyId, companySymbol, endDate, new BigDecimal("200.00"), new BigDecimal("150.00"));
        List<RollingMetricDTO> rollingsMetricDTOs = new ArrayList<>();
        rollingsMetricDTOs.add(rmDTO1); 
        rollingsMetricDTOs.add(rmDTO2); 

        // Act
        List<RollingMetricDTO> results = analyticsService.findRollingMetricsByCompanyIdAndDateRange(companySymbol, startDate, endDate);

        // Assert
        assertThat(results).hasSize(31);

        RollingMetricDTO session1 = results.get(0);
        assertThat(session1.getClosePrice()).isEqualByComparingTo(new BigDecimal("100.0000"));
        assertThat(session1.getRollingAvg30()).isEqualByComparingTo(new BigDecimal("100.0000"));

        RollingMetricDTO session30 = results.get(29);
        assertThat(session30.getClosePrice()).isEqualByComparingTo(new BigDecimal("100.0000"));
        assertThat(session30.getRollingAvg30()).isEqualByComparingTo(new BigDecimal("100.0000"));

        RollingMetricDTO session31 = results.get(30);
        assertThat(session31.getClosePrice()).isEqualByComparingTo(new BigDecimal("400.0000"));
        assertThat(session31.getRollingAvg30()).isEqualByComparingTo(new BigDecimal("110.0000"));

        assertThat(results).extracting(
            RollingMetricDTO::getPriceDate,
            RollingMetricDTO::getClosePrice,
            RollingMetricDTO::getRollingAvg30
        ).doesNotContainNull();

        assertThat(results).isSortedAccordingTo(Comparator.comparing(RollingMetricDTO::getPriceDate));
    } 

    @Test
    @Sql(scripts = "/db/cleanup.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = "/db/insert_risk_current_prices.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void getRiskLevel_givenWrongAPIKEY_ShouldReturn403() throws InterruptedException{
        RiskEvaluationResponse reResonse = new RiskEvaluationResponse();
        reResonse.setCompanyId(1);
        reResonse.setName("Nikkei225");
        reResonse.setEvaluatedAt(LocalDate.now());

        
        given()
            .relaxedHTTPSValidation()
            .header("API_KEY", "WRONG_KEY")
            .queryParam("companySymbol", "Nikkei225")
            .when()
                .get("/api/v1/riskEvaluation/risklevel") ///api/v1/riskEvaluation/risklevel?companySymbol=DAX
            .then()
                .statusCode(403);
    }  

    @Test
    void getRiskLevel_WhenApiKeyMissing_ShouldReturn403() {
        given()
            .queryParam("companySymbol", "Nikkei225")
        .when()
            .get("/api/v1/riskEvaluation/risklevel")
        .then()
            .statusCode(403);
    }

    @Test
    @Sql(scripts = "/db/cleanup.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = "/db/insert_test_prices.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void findRollingMetricsByCompanyIdAndDateRange_ShouldMapAllDtoFieldsWithPreservedPrecision(){
        String companySymbol = "Nikkei225";
        LocalDate startDate = LocalDate.parse("2026-07-01");
        LocalDate endDate = LocalDate.parse("2026-07-31");

        List<RollingMetricDTO> rollingMetricDTOs = analyticsService.findRollingMetricsByCompanyIdAndDateRange(companySymbol,startDate,endDate);

        // Verify list is populated
        assertThat(rollingMetricDTOs).isNotEmpty();

        // Verify all records have non-null fields
        assertThat(rollingMetricDTOs).extracting(
            RollingMetricDTO::getCompanyId,
            RollingMetricDTO::getName,
            RollingMetricDTO::getPriceDate,
            RollingMetricDTO::getClosePrice,
            RollingMetricDTO::getRollingAvg30
        ).doesNotContainNull();

        // Verify precision preservation on BigDecimal (no rounding/truncation)
        RollingMetricDTO firstRecord = rollingMetricDTOs.get(0);
        assertThat(firstRecord.getClosePrice())
            .isEqualByComparingTo(new BigDecimal("100.0000"));
    }

    @Test
    @Sql(scripts = "/db/cleanup.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = "/db/insert_test_prices.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void getRiskLevel_WhenValidSymbolAndDataAvailable_ShouldReturn200AndCalculatedEvaluation(){
        RiskEvaluationResponse expectetResponse = new RiskEvaluationResponse();
        expectetResponse.setCompanyId(1);
        expectetResponse.setName("Nikkei225");
        expectetResponse.setEvaluatedAt(LocalDate.now(clock));
        expectetResponse.setDailyReturn(new BigDecimal("3.0000"));
        expectetResponse.setSma(new BigDecimal("0.2499999999999999999999999999999998"));
        expectetResponse.setVolumeSpike(new BigDecimal("1.00"));
        expectetResponse.setRollingAvg(new BigDecimal("0.09090909090909090909090909090909091"));

        expectetResponse.setDailyReturnRiskLevel(RiskLevel.CRITICAL);
        expectetResponse.setSmaRiskLevel(RiskLevel.CRITICAL); //LOW??
        expectetResponse.setRollingAvgRiskLevel(RiskLevel.HIGH);
        expectetResponse.setVolumeSpikeRiskLevel(RiskLevel.LOW);

        expectetResponse.setOverallRiskLevel(RiskLevel.CRITICAL);
        expectetResponse.setActiveRiskDrivers(List.of("dailyReturn", "sma", "rollingAvg"));
        expectetResponse.setPrimaryRiskDriver("dailyReturn");

        RiskEvaluationResponse actualResponse = given()
                                                    .relaxedHTTPSValidation()
                                                    .header("API_KEY", "OZpAJ)C>2>EBWe9ee<R|f[%RpOucF31")
                                                    .queryParam("companySymbol", "Nikkei225")
                                                .when()
                                                    .get("/api/v1/riskEvaluation/risklevel") ///api/v1/riskEvaluation/risklevel?companySymbol=DAX
                                                .then()
                                                    .statusCode(200)
                                                    .extract()
                                                    .as(RiskEvaluationResponse.class);

        assertThat(actualResponse)
            .usingRecursiveComparison()
            .withComparatorForType(BigDecimal::compareTo, BigDecimal.class)
            .isEqualTo(expectetResponse);
    }

    @Test
    @Sql(scripts = "/db/cleanup.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = "/db/insert_test_prices.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void getAvg30_WhenValidSymbolAndDataAvailable_ShouldReturn200AndCalculatedRollingMetrics(){
        List<RollingMetricDTO> expectedList = new ArrayList<>();

        // Days 1 to 30: closePrice = 100.0000, rollingAvg30 = 100.0000000000000000
        for (int day = 1; day <= 30; day++) {
            expectedList.add(new RollingMetricDTO(
                1,
                "Nikkei225",
                LocalDate.of(2026, 7, day),
                new BigDecimal("100.0000"),
                new BigDecimal("100.0000000000000000")
            ));
        }

        // Day 31: closePrice = 400.0000, rollingAvg30 = 110.0000000000000000
        expectedList.add(new RollingMetricDTO(
            1,
            "Nikkei225",
            LocalDate.of(2026, 7, 31),
            new BigDecimal("400.0000"),
            new BigDecimal("110.0000000000000000")
        ));

        List<RollingMetricDTO> actualResponse = given()
                                                    .relaxedHTTPSValidation()
                                                    .header("API_KEY", "OZpAJ)C>2>EBWe9ee<R|f[%RpOucF31")
                                                    .queryParam("companySymbol", "Nikkei225")
                                                .when()
                                                    .get("/api/v1/analytics/avg30") ///api/v1/riskEvaluation/risklevel?companySymbol=DAX
                                                .then()
                                                    .statusCode(200)
                                                    .extract()
                                                    .as(new TypeRef<List<RollingMetricDTO>>(){});
        assertThat(actualResponse)
            .usingRecursiveComparison()
            .withComparatorForType(BigDecimal::compareTo, BigDecimal.class)
            .isEqualTo(expectedList);
        }
}
