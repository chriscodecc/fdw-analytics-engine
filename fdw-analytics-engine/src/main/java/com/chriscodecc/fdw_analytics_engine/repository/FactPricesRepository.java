package com.chriscodecc.fdw_analytics_engine.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.chriscodecc.fdw_analytics_engine.model.FactPrices;

public interface FactPricesRepository extends JpaRepository<FactPrices, Long>{
    
}
