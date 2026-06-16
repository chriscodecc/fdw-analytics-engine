package com.chriscodecc.fdw_analytics_engine.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.crossstore.ChangeSetPersister.NotFoundException;
import org.springframework.stereotype.Service;

import com.chriscodecc.fdw_analytics_engine.model.DimCompany;
import com.chriscodecc.fdw_analytics_engine.model.FactPrices;
import com.chriscodecc.fdw_analytics_engine.repository.DimCompanyRepository;
import com.chriscodecc.fdw_analytics_engine.repository.DimDateRepository;
import com.chriscodecc.fdw_analytics_engine.repository.FactPricesRepository;

import jakarta.persistence.EntityNotFoundException;

@Service
public class AnalyticsService {
    
    private final FactPricesRepository factPricesRepository;
    private final DimCompanyRepository dimCompanyRepository;
    private final DimDateRepository dimDateRepository;

    private final BigDecimal DAILY_RETURN_THRESHOLD = new BigDecimal(0.05);

    public AnalyticsService(FactPricesRepository factPricesRepository, DimCompanyRepository dimCompanyRepository,  DimDateRepository dimDateRepository){
        this.factPricesRepository = factPricesRepository;
        this.dimCompanyRepository = dimCompanyRepository;
        this.dimDateRepository = dimDateRepository;
    }

    public BigDecimal dailyReturn(String companySymbol) throws EntityNotFoundException{
        LocalDate today = LocalDate.now(); 
        
        Integer companyId = dimCompanyRepository.findBySymbol(companySymbol)
                                                            .orElseThrow(() -> new EntityNotFoundException("Company not found!"))
                                                .getId();
    
        
        List<FactPrices> lastesPrices = factPricesRepository.findLatestPriceBeforeDate(companyId, today);

        if(lastesPrices.size() < 2){
            throw new DataIntegrityViolationException ("Critical error: Data could not be loaded.");
        }

        FactPrices todayFactPrices = lastesPrices.get(0);                
        FactPrices yesterdaysFactPrices = lastesPrices.get(1);
        
        BigDecimal closeDiff = todayFactPrices.getClosePrice().subtract(yesterdaysFactPrices.getClosePrice());

        return closeDiff.divide(yesterdaysFactPrices.getClosePrice(), 4, RoundingMode.HALF_EVEN);
    }

    public Boolean dailyReturnThresholdTrigger(BigDecimal dailyReturn){
        if(DAILY_RETURN_THRESHOLD.compareTo(dailyReturn.abs()) <= 0){
            return true;
        }
        return false;
    }
}
