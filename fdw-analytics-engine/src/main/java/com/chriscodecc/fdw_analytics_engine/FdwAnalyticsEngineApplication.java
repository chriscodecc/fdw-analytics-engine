package com.chriscodecc.fdw_analytics_engine;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import com.chriscodecc.fdw_analytics_engine.model.DimDate;
import com.chriscodecc.fdw_analytics_engine.model.FactPrices;
import com.chriscodecc.fdw_analytics_engine.repository.DimCompanyRepository;
import com.chriscodecc.fdw_analytics_engine.repository.DimDateRepository;
import com.chriscodecc.fdw_analytics_engine.repository.FactPricesRepository;

@SpringBootApplication
public class FdwAnalyticsEngineApplication {
	DimCompanyRepository dimCompanyRepository;

	public static void main(String[] args) {
		SpringApplication.run(FdwAnalyticsEngineApplication.class, args);
		
	}

	@Bean
	public CommandLineRunner demo(FactPricesRepository repository) {
		return (args) -> {
			System.out.println("--------------------------------------------");
            System.out.println("SPRING BOOT GESTARTET – STARTE DB-TEST... ");
            System.out.println("--------------------------------------------");

			repository.findAll().forEach(thisfact -> {
				System.out.println("Preis gefunden: " + thisfact.getCompanyId().getName() + " ; " + thisfact.getVolume());
			});

			System.out.println("--------------------------------------------");
		};
	}



}
