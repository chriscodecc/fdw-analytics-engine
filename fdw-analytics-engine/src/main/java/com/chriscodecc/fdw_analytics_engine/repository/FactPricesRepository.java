package com.chriscodecc.fdw_analytics_engine.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.chriscodecc.fdw_analytics_engine.model.FactPrices;

public interface FactPricesRepository extends JpaRepository<FactPrices, Long>{
    
    Optional<FactPrices> findByDimCompanyIdAndDimDateFullDate(Integer companyId, LocalDate fullDate);

    @Query("SELECT f FROM FactPrices f " +
           "WHERE f.dimCompany.id = :companyId " +
           "AND f.dimDate.fullDate <= :date " +
           "ORDER BY f.dimDate.fullDate DESC LIMIT 2")
    List<FactPrices> findLatestPriceBeforeDate(
        @Param("companyId") Integer companyId, 
        @Param("date") LocalDate date
    );

    @Query("SELECT f FROM FactPrices f " + 
          "WHERE f.dimCompany.id = :companyId " + 
          "AND f.dimDate.fullDate <= :date " + 
          "AND f.dimDate.fullDate BETWEEN :startDate AND :date " + 
          "ORDER BY f.dimDate.fullDate DESC")
    List<FactPrices> findLatestPricesForLastPastDays(
        @Param("companyId") Integer companyId, 
        @Param("date") LocalDate date, 
        @Param("startDate") LocalDate startDate);

    Optional<FactPrices> findFirstByDimCompanySymbolOrderByDimDateFullDateDesc(String companySymbol);
}
