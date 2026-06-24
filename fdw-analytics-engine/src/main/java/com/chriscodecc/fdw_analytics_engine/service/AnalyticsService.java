package com.chriscodecc.fdw_analytics_engine.service;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.cglib.core.Local;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import com.chriscodecc.fdw_analytics_engine.model.FactPrices;
import com.chriscodecc.fdw_analytics_engine.repository.DimCompanyRepository;
import com.chriscodecc.fdw_analytics_engine.repository.DimDateRepository;
import com.chriscodecc.fdw_analytics_engine.repository.FactPricesRepository;

import jakarta.persistence.EntityNotFoundException;

/**
* The AnalyticsService is the core of the calculation logic. 
* It analyzes historical market data, calculates moving averages (SMA)
* and percentage price changes, and identifies unusual volume spikes. 
*/
@Service
public class AnalyticsService {
    
    private final FactPricesRepository factPricesRepository;
    private final DimCompanyRepository dimCompanyRepository;
    private final DimDateRepository dimDateRepository;

    private final BigDecimal DAILY_RETURN_THRESHOLD = new BigDecimal(0.1);
    private static int SMA_PERIOD_DAYS = 8;


    public AnalyticsService(FactPricesRepository factPricesRepository, DimCompanyRepository dimCompanyRepository,  DimDateRepository dimDateRepository){
        this.factPricesRepository = factPricesRepository;
        this.dimCompanyRepository = dimCompanyRepository;
        this.dimDateRepository = dimDateRepository;
    }

    public BigDecimal dailyReturn(String companySymbol, LocalDate today) throws EntityNotFoundException{
        
        Integer companyId = dimCompanyRepository.findBySymbol(companySymbol)
                                                            .orElseThrow(() -> new EntityNotFoundException("Company not found!"))
                                                .getId();
    
        
        List<FactPrices> lastestPrices = factPricesRepository.findLatestPriceBeforeDate(companyId, today);

        if(lastestPrices.size() < 2){
            throw new DataIntegrityViolationException ("Critical error: Data could not be loaded.");
        }

        FactPrices todayFactPrices = lastestPrices.get(0);                
        FactPrices yesterdaysFactPrices = lastestPrices.get(1);
        
        BigDecimal closeDiff = todayFactPrices.getClosePrice().subtract(yesterdaysFactPrices.getClosePrice());

        return closeDiff.divide(yesterdaysFactPrices.getClosePrice(), 4, RoundingMode.HALF_EVEN);
    }

    public BigDecimal dailyReturn(String companySymvbol){
        return dailyReturn(companySymvbol, LocalDate.now());
    }

    public Boolean checkDailyReturnThreshold(String companySymbol, LocalDate today){
        BigDecimal dailyReturn = dailyReturn(companySymbol, today);
        
        return dailyReturn.abs().compareTo(DAILY_RETURN_THRESHOLD) >= 0;
    }    
    
    public Boolean checkDailyReturnThreshold(String companySymbol){
        return checkDailyReturnThreshold(companySymbol, LocalDate.now());
    }

    public BigDecimal simpleMovingAverageSeven(String companySymbol, LocalDate today){
        Integer companyId = dimCompanyRepository.findBySymbol(companySymbol)
                                                    .orElseThrow(() -> new EntityNotFoundException("Company not found!"))
                                                .getId();
        List<FactPrices> historicalPriceData = factPricesRepository.findLatestPricesForLastPastDays(companyId, today, today.minusDays(SMA_PERIOD_DAYS));
        if(historicalPriceData.isEmpty()){
            throw new IllegalArgumentException();
        }
        BigDecimal simpleMovingAverage = BigDecimal.ZERO;

        for(FactPrices price : historicalPriceData){
            simpleMovingAverage = simpleMovingAverage.add(price.getClosePrice());
        }
        simpleMovingAverage = simpleMovingAverage.divide(new BigDecimal(historicalPriceData.size()), MathContext.DECIMAL128);

        return simpleMovingAverage;
    }

    public boolean simpleMovingAverageAlert(String companySymbol, LocalDate today){
        BigDecimal sma = simpleMovingAverageSeven(companySymbol, today);
        FactPrices factPrices = factPricesRepository.findFirstByDimCompanySymbolOrderByDimDateFullDateDesc(companySymbol)
                                                        .orElseThrow(() -> new EntityNotFoundException("Company not found!"));

        return factPrices.getLowPrice().compareTo(sma) < 0;
    }

    public BigDecimal simpleMovingAverageSeven(String companySymbol){
        LocalDate today = LocalDate.now();
        return simpleMovingAverageSeven(companySymbol, today);
    }

    public boolean simpleMovingAverageAlert(String companySymbol){
        LocalDate today = LocalDate.now();
        return simpleMovingAverageAlert(companySymbol, today);
    }

    private List<BigDecimal> getVolumeDataIncludingToday(String companySymbol,LocalDate today, long days){
        List<BigDecimal> volumeForPastDays = new ArrayList<>();
        int companyId = dimCompanyRepository.findBySymbol(companySymbol)
                                                .orElseThrow(() -> new EntityNotFoundException("Company not found"))
                                            .getId();
        List<FactPrices> factPricesList = factPricesRepository.findLatestPricesForLastPastDays(companyId, today, today.minusDays(days));

        if(!factPricesList.isEmpty()){
            for (FactPrices factPrices : factPricesList) {
                volumeForPastDays.add(new BigDecimal(factPrices.getVolume()));
            }
            return volumeForPastDays;
        } else {
            throw new IllegalArgumentException();
        }
    }

    private List<BigDecimal> getVolumeDataIncludingToday(String companySymbol){
        LocalDate today = LocalDate.now();
        return getVolumeDataIncludingToday(companySymbol, today, SMA_PERIOD_DAYS);
    }

    private List<BigDecimal> getVolumeDataIncludingToday(String companySymbol, LocalDate today){
        return getVolumeDataIncludingToday(companySymbol, today, SMA_PERIOD_DAYS);
    }

    private BigDecimal averageVolume(List<BigDecimal> historicalVolumeData){
        BigDecimal avgVolume = BigDecimal.ZERO;
        for (BigDecimal volume : historicalVolumeData) {
            avgVolume = avgVolume.add(volume);
        }
        return avgVolume.divide(new BigDecimal(historicalVolumeData.size()), 2, RoundingMode.HALF_UP);
    }

    private boolean evaluateVolumeSpike(BigDecimal currentVolumne, BigDecimal avgVolume){
        return currentVolumne.compareTo(avgVolume.multiply(new BigDecimal(2))) >= 0;
    }

    public boolean volumeSpikeAlert(String companySymbol, LocalDate today){
        List<BigDecimal> historicalVolumeData = getVolumeDataIncludingToday(companySymbol, today);
        BigDecimal currentVolume = historicalVolumeData.remove(0);
        BigDecimal avgVolume = averageVolume(historicalVolumeData);
        
        return evaluateVolumeSpike(currentVolume, avgVolume);
    } 

    public boolean volumeSpikeAlert(String companySymbol){
        LocalDate today = LocalDate.now();
        return volumeSpikeAlert(companySymbol, today);
    }
}
