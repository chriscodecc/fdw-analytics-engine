package com.chriscodecc.fdw_analytics_engine.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.chriscodecc.fdw_analytics_engine.model.DimDate;
import java.util.List;


public interface DimDateRepository extends JpaRepository<DimDate, Long>{
    

}
