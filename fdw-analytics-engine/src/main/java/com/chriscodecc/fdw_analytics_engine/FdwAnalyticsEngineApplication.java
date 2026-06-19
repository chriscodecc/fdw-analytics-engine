package com.chriscodecc.fdw_analytics_engine;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import com.chriscodecc.fdw_analytics_engine.repository.DimCompanyRepository;
import com.chriscodecc.fdw_analytics_engine.service.AnalyticsService;

@SpringBootApplication
public class FdwAnalyticsEngineApplication {
	DimCompanyRepository dimCompanyRepository;

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
	
			System.out.println("SMA ALERT DAX: " + service.simpleMovingAverageAlert("Nikkei225"));
			System.out.println("--------------------------------------------");
		};
	}



}
