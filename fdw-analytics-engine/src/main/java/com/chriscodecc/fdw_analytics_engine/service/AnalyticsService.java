package com.chriscodecc.fdw_analytics_engine.service;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.crossstore.ChangeSetPersister.NotFoundException;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.jsf.FacesContextUtils;

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

    private final BigDecimal DAILY_RETURN_THRESHOLD = new BigDecimal(0.08);
    private final BigDecimal SMA_THRESHOLD = new BigDecimal(10000.0);

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

    public Boolean checkDailyReturnThreshold(String companySymbol){
        BigDecimal dailyReturn = dailyReturn(companySymbol);
        
        return dailyReturn.abs().compareTo(DAILY_RETURN_THRESHOLD) <= 0;
    }                                   

    public BigDecimal simpleMovingAverageSeven(String companySymbol){
        LocalDate today = LocalDate.now(); // , LocalDate today
        Integer companyId = dimCompanyRepository.findBySymbol(companySymbol)
                                                    .orElseThrow(() -> new EntityNotFoundException("Company not found!"))
                                                .getId();
        List<FactPrices> historicalPrices = factPricesRepository.findLatesPricesForLastXDays(companyId, today, today.minusDays(7));
        if(historicalPrices.isEmpty()){
            throw new IllegalArgumentException();
        }
        BigDecimal simpleMovingAverage = new BigDecimal(0);

        for(FactPrices price : historicalPrices){
            simpleMovingAverage = simpleMovingAverage.add(price.getClosePrice());
        }
        simpleMovingAverage = simpleMovingAverage.divide(new BigDecimal(historicalPrices.size()), MathContext.DECIMAL128);

        return simpleMovingAverage;
    }

    public boolean simpleMovingAverageAlert(String companySymbol){
        BigDecimal sma = simpleMovingAverageSeven(companySymbol);
        FactPrices factPrices = factPricesRepository.findFirstByDimCompanySymbolOrderByDimDateFullDateDesc(companySymbol)
                                                        .orElseThrow(() -> new EntityNotFoundException("Company not found!"));

        return factPrices.getLowPrice().compareTo(sma) < 0;
    }

    public List<Long> getVolumeHistoryForPastDays(String companySymbol, long days){
        LocalDate today = LocalDate.now();
        List<Long> volumeForPastDays = new ArrayList<>();
        int companyId = dimCompanyRepository.findBySymbol(companySymbol)
                                                .orElseThrow(() -> new EntityNotFoundException("Company not found"))
                                            .getId();
        List<FactPrices> factPricesList = factPricesRepository.findLatesPricesForLastXDays(companyId, today, today.minusDays(days));

        if(!factPricesList.isEmpty()){
            for (FactPrices factPrices : factPricesList) {
                volumeForPastDays.add(factPrices.getVolume());
            }
            return volumeForPastDays;
        } else {
            throw new IllegalArgumentException();
        }
    }

    public List<Long> getVolumeHistoryForPastDays(String companySymbolString){
        return getVolumeHistoryForPastDays(companySymbolString, 7);
    }
}
