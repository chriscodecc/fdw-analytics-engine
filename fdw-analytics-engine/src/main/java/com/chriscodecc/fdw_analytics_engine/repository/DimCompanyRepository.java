package com.chriscodecc.fdw_analytics_engine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.chriscodecc.fdw_analytics_engine.model.DimCompany;

@Repository
public interface DimCompanyRepository extends JpaRepository<DimCompany, Long>{

    
    
}
