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

import com.chriscodecc.fdw_analytics_engine.model.DimCompany;
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

    //#region daily return #########################################################################

    /**
     * Calculates the daily percentage return for a given company symbol.
     * Uses the two most recent available prices before the specified date (e.g., skipping weekends).
     *
     * @param companySymbol the unique ticker symbol of the company (e.g., "DAX")
     * @param today the reference date to look back from
     * @return the daily return as a decimal ratio (e.g., 0.05 for a 5% gain)
     * @throws EntityNotFoundException if no company matches the provided symbol
     * @throws DataIntegrityViolationException if fewer than two historical prices are found
     */
    public BigDecimal dailyReturn(String companySymbol, LocalDate today) throws EntityNotFoundException{   
        Integer companyId = findCompanyBySymbol(companySymbol).getId();
    
        List<FactPrices> lastestPrices = factPricesRepository.findLatestPriceBeforeDate(companyId, today);

        if(lastestPrices.size() < 2){
            throw new DataIntegrityViolationException ("Critical error: Data could not be loaded.");
        }

        FactPrices todayFactPrices = lastestPrices.get(0);                
        FactPrices yesterdaysFactPrices = lastestPrices.get(1);
        
        BigDecimal closeDiff = todayFactPrices.getClosePrice().subtract(yesterdaysFactPrices.getClosePrice());

        return closeDiff.divide(yesterdaysFactPrices.getClosePrice(), 4, RoundingMode.HALF_EVEN);
    }

    public BigDecimal dailyReturn(String companySymbol){
        return dailyReturn(companySymbol, LocalDate.now());
    }
    
    /**
     * Calculates the daily return and checks if its absolute deviation 
     * is greater than or equal to the predefined threshold.
     * 
     * @param companySymbol the unique ticker symbol of the company (e.g., "DAX")
     * @param today the reference date for the daily return calculation
     * @return true if the absolute daily return meets or exceeds the threshold, false otherwise
     */
    public Boolean checkDailyReturnThreshold(String companySymbol, LocalDate today){
        BigDecimal dailyReturn = dailyReturn(companySymbol, today);
        
        return dailyReturn.abs().compareTo(DAILY_RETURN_THRESHOLD) >= 0;
    }    
    
    public Boolean checkDailyReturnThreshold(String companySymbol){
        return checkDailyReturnThreshold(companySymbol, LocalDate.now());
    }

    //#endregion daily Return #########################################################################

    //#region SMA

    /**
     * Calculates the Simple Moving Average (SMA) of the closing prices
     * over the configured number of past days (SMA_PERIOD_DAYS).
     *
     * @param companySymbol the unique ticker symbol of the company (e.g., "DAX")
     * @param today the end date of the historical period to calculate the average for
     * @return the calculated moving average as a BigDecimal
     * @throws EntityNotFoundException if no company matches the provided symbol
     * @throws IllegalArgumentException if no historical price data is found for the period
     */
    public BigDecimal calculateSma(DimCompany dimComp, LocalDate today){

        List<FactPrices> historicalPriceData = factPricesRepository.findLatestPricesForLastPastDays(dimComp.getId(), today, today.minusDays(SMA_PERIOD_DAYS));
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

    public BigDecimal calculateSma(DimCompany dimCompany){
        LocalDate today = LocalDate.now();
        return calculateSma(dimCompany, today);
    }
    
    /**
     * Compares the SMA to a given date
     * 
     * @param companySymbol the unique ticker symbol of the company (e.g., "DAX")
     * @param today the start day for calculating and comparing the SMA (today if not hand over)
     * @return 
     */ 
    public boolean simpleMovingAverageAlert(String companySymbol, LocalDate today){
        DimCompany dimComp = findCompanyBySymbol(companySymbol);

        BigDecimal sma = calculateSma(dimComp, today);
        FactPrices factPrices = factPricesRepository.findFirstByDimCompanyIdOrderByDimDateFullDateDesc(dimComp.getId())
                                                        .orElseThrow(() -> new EntityNotFoundException("No prices found for " + dimComp.getName() + " !"));

        return factPrices.getLowPrice().compareTo(sma) < 0;
    }

    public boolean simpleMovingAverageAlert(String companySymbol){
        LocalDate today = LocalDate.now();
        return simpleMovingAverageAlert(companySymbol, today);
    }

    //#endregion SMA #########################################################################

    private List<BigDecimal> getVolumeDataIncludingToday(String companySymbol,LocalDate today, long days){
        List<BigDecimal> volumeForPastDays = new ArrayList<>();
        int companyId = findCompanyBySymbol(companySymbol).getId();
        
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

    private DimCompany findCompanyBySymbol(String companySymbol){
        return dimCompanyRepository.findBySymbol(companySymbol).orElseThrow(() -> new EntityNotFoundException("Company not Found!"));
    }
}
