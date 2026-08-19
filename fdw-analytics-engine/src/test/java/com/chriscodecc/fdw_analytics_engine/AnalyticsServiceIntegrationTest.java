package com.chriscodecc.fdw_analytics_engine;

import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.format.datetime.DateFormatter;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.jdbc.Sql;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.chriscodecc.fdw_analytics_engine.dto.RollingMetricDTO;
import com.chriscodecc.fdw_analytics_engine.repository.FactPricesRepository;
import com.chriscodecc.fdw_analytics_engine.service.AnalyticsService;

import io.restassured.RestAssured;
import jakarta.transaction.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;


//  mvn test -Dtest=AnalyticsServiceIntegrationTest

@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Transactional
public class AnalyticsServiceIntegrationTest {
    
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
    private Integer port;

    @Autowired
    FactPricesRepository factPricesRepository; 
    @Autowired
    AnalyticsService analyticsService;
    
    @BeforeEach
    void beforeEach(){
        RestAssured.baseURI = "http://localhost:" + port;   
    }

    @Test
    @Sql(scripts = "/db/cleanup.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = "/db/insert_test_prices.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void containerIsRunningAndInitialized() {
        assertThat(postgres.isRunning()).isTrue();
        assertThat(factPricesRepository.count()).isGreaterThanOrEqualTo(0);
    }

    @Test
    @Sql(scripts = "/db/cleanup.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = "/db/insert_test_prices.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void findRollingMetricsByCompanyIdAndDateRange(){
        Long companyId = 1L;
        LocalDate startDate = LocalDate.parse("2026-07-01");
        LocalDate endDate = LocalDate.parse("2026-07-31");

        RollingMetricDTO rmDTO1 = new RollingMetricDTO(companyId, "Nikkei225", startDate, new BigDecimal("100.00"), new BigDecimal("100.00"));
        RollingMetricDTO rmDTO2 = new RollingMetricDTO(companyId, "Nikkei225", endDate, new BigDecimal("200.00"), new BigDecimal("150.00"));
        List<RollingMetricDTO> rollingsMetricDTOs = new ArrayList<>();
        rollingsMetricDTOs.add(rmDTO1); 
        rollingsMetricDTOs.add(rmDTO2); 

        // Act
        List<RollingMetricDTO> results = analyticsService.findRollingMetricsByCompanyIdAndDateRange(companyId, startDate, endDate);

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
    } // NEW BRANCHASDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDD!!!!!!!!!!!!!!!!!!!!!!
}
