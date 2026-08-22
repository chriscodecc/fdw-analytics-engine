package com.chriscodecc.fdw_analytics_engine.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.chriscodecc.fdw_analytics_engine.dto.RollingMetricDTO;
import com.chriscodecc.fdw_analytics_engine.dto.RollingMetricProjection;
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

    Optional<FactPrices> findFirstByDimCompanyIdOrderByDimDateFullDateDesc(Integer companyId);


    @Query(nativeQuery = true, value="" + 
           "WITH rolling_data AS (" +  
                "SELECT f.company_id, dc.name, dd.full_date, f.close_price, " + 
                        "AVG(f.close_price) OVER (" +
                            "PARTITION BY f.company_id " + 
                            "ORDER BY dd.full_date " +
                            "ROWS BETWEEN 29 PRECEDING AND CURRENT ROW) " + 
                        "AS avg_for_me " + 
                "FROM fact_prices f " +
                "JOIN dim_date dd ON dd.date_id = f.date_id " + 
                "JOIN dim_company dc ON dc.company_id = f.company_id " +
                "WHERE f.company_id = :companyId " +
            ")" +
            "SELECT company_id, name, full_date, close_price, avg_for_me " +
            "FROM rolling_data " + 
            "WHERE full_date BETWEEN :startDate AND :endDate " + 
            "ORDER BY full_date ASC; ")
    List<RollingMetricProjection> findRollingMetricsByCompanyIdAndDateRange(Integer companyId, LocalDate startDate, LocalDate endDate);
}   
