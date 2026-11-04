package com.chriscodecc.fdw_analytics_engine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.chriscodecc.fdw_analytics_engine.model.DimCompany;
import java.util.Optional;


public interface DimCompanyRepository extends JpaRepository<DimCompany, Long>{

    Optional<DimCompany> findBySymbol(String symbol);
    
}
