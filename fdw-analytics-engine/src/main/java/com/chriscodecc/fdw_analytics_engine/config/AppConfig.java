package com.chriscodecc.fdw_analytics_engine.config;

import java.time.Clock;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AppConfig {
    
    @Bean 
    public Clock clock(){
        return Clock.systemUTC();
    }
}
