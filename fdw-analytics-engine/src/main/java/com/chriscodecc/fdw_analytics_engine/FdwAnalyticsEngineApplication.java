package com.chriscodecc.fdw_analytics_engine;

import com.chriscodecc.fdw_analytics_engine.controller.AnalyticsController;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import com.chriscodecc.fdw_analytics_engine.repository.DimCompanyRepository;
import com.chriscodecc.fdw_analytics_engine.service.AnalyticsService;

@SpringBootApplication
public class FdwAnalyticsEngineApplication {
	private final AnalyticsService analyticsService;
    private final AnalyticsController analyticsController;
    DimCompanyRepository dimCompanyRepository;

    FdwAnalyticsEngineApplication(AnalyticsController analyticsController, AnalyticsService analyticsService) {
        this.analyticsController = analyticsController;
        this.analyticsService = analyticsService;
    }

	public static void main(String[] args) {
		SpringApplication.run(FdwAnalyticsEngineApplication.class, args);
	
		
	}

	

	// docker-compose run --rm data-job python src/main.py
	@Bean
	public CommandLineRunner demo(AnalyticsService service) {
		return (args) -> {
			System.out.println("--------------------------------------------");
            System.out.println("SPRING BOOT GESTARTET - STARTE DB-TEST... ");
            System.out.println("--------------------------------------------");
	
			System.out.println("7 Days volume for NIKKEI225: ");

			System.out.println("--------------------------------------------");
		};
	}



}
