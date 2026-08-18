package com.chriscodecc.fdw_analytics_engine.service;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cglib.core.Local;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import com.chriscodecc.fdw_analytics_engine.dto.RollingMetricDTO;
import com.chriscodecc.fdw_analytics_engine.dto.RollingMetricProjection;
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
    private final BigDecimal SMA_THRESHOLD = new BigDecimal(0.10);
    private static int SMA_PERIOD_DAYS = 8;
    private static int VOLUME_PERIOD_DAYS = 8;

    @Autowired
    private javax.sql.DataSource dataSource;


    public AnalyticsService(FactPricesRepository factPricesRepository, DimCompanyRepository dimCompanyRepository,  DimDateRepository dimDateRepository){
        this.factPricesRepository = factPricesRepository;
        this.dimCompanyRepository = dimCompanyRepository;
        this.dimDateRepository = dimDateRepository;
    }

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
    
        List<FactPrices> latestPrices = factPricesRepository.findLatestPriceBeforeDate(companyId, today);

        if(latestPrices.size() < 2){
            throw new DataIntegrityViolationException ("Critical error: Data could not be loaded.");
        }

        FactPrices todayFactPrices = latestPrices.get(0);                
        FactPrices yesterdaysFactPrices = latestPrices.get(1);
        
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
    public boolean checkDailyReturnThreshold(String companySymbol, LocalDate today){
        BigDecimal dailyReturn = dailyReturn(companySymbol, today);
        
        return dailyReturn.abs().compareTo(DAILY_RETURN_THRESHOLD) >= 0;
    }    
    
    public boolean checkDailyReturnThreshold(String companySymbol){
        return checkDailyReturnThreshold(companySymbol, LocalDate.now());
    }

    /**
     * Calculates the Simple Moving Average (SMA) of the closing prices
     * over the configured number of past days (SMA_PERIOD_DAYS).
     *
     * @param dimComp the company entity for which the moving average is calculated
     * @param today the end date of the historical period to calculate the average for
     * @return the calculated moving average as a BigDecimal
     * @throws EntityNotFoundException if no company matches the provided symbol
     * @throws IllegalArgumentException if no historical price data is found for the period
     */
    public BigDecimal calculateSma(DimCompany dimComp, LocalDate today){

        List<FactPrices> historicalPriceData = factPricesRepository.findLatestPricesForLastPastDays(dimComp.getId(), today, today.minusDays(SMA_PERIOD_DAYS));
        if(historicalPriceData.isEmpty()){
            throw new IllegalArgumentException("No historical data available!");
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
     * Compares the SMA to the low Price on a given date (today unless otherwise specified) 
     * 
     * @param companySymbol the unique ticker symbol of the company (e.g., "DAX")
     * @param today the start day for calculating and comparing the SMA (today if not hand over)
     * @return true if the low price falls below the SMA threshold, false otherwise
     */ 
    public boolean simpleMovingAverageAlert(String companySymbol, LocalDate today){
        DimCompany dimComp = findCompanyBySymbol(companySymbol);

        BigDecimal sma = calculateSma(dimComp, today);
        FactPrices factPrices = factPricesRepository.findFirstByDimCompanyIdOrderByDimDateFullDateDesc(dimComp.getId())
                                                        .orElseThrow(() -> new EntityNotFoundException("No prices found for " + dimComp.getName() + " !"));

        BigDecimal thresholdModifier = BigDecimal.ONE.subtract(SMA_THRESHOLD);
        BigDecimal smaWithThreshold = sma.multiply(thresholdModifier);

        return factPrices.getLowPrice().compareTo(smaWithThreshold) < 0;
    }

    public boolean simpleMovingAverageAlert(String companySymbol){
        LocalDate today = LocalDate.now();
        return simpleMovingAverageAlert(companySymbol, today);
    }


    /**
     * Gets the volume data for the past VOLUME_PERIOD_DAYS for the given company.
     * 
     * @param dimComp the company entity for which the volume data is fetched
     * @param today the reference date from which the data is fetched
     * @param days the time period for which the data is fetched (VOLUME_PERIOD_DAYS)
     * @return a list containing the historical volume data
     * @throws IllegalArgumentException if no historical data is available for the period
     */
    private List<BigDecimal> getVolumeDataIncludingToday(DimCompany dimComp,LocalDate today, long days){
        List<BigDecimal> volumeForPastDays = new ArrayList<>();
        int companyId = dimComp.getId();

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

    private List<BigDecimal> getVolumeDataIncludingToday(DimCompany dimComp){
        LocalDate today = LocalDate.now();
        return getVolumeDataIncludingToday(dimComp, today, VOLUME_PERIOD_DAYS);
    }

    private List<BigDecimal> getVolumeDataIncludingToday(DimCompany dimComp, LocalDate today){
        return getVolumeDataIncludingToday(dimComp, today, VOLUME_PERIOD_DAYS);
    }

    /**
     * Calculates the average volume.
     * @param historicalVolumeData a list that contains the historical volume data
     * @return the average volume of the given list as a BigDecimal
     */
    private BigDecimal averageVolume(List<BigDecimal> historicalVolumeData){
        BigDecimal avgVolume = BigDecimal.ZERO;
        for (BigDecimal volume : historicalVolumeData) {
            avgVolume = avgVolume.add(volume);
        }
        return avgVolume.divide(new BigDecimal(historicalVolumeData.size()), 2, RoundingMode.HALF_UP);
    }

    /**
     * Checks if the current volume constitutes a spike compared to the average.
     * 
     * @param currentVolume the calculated volume of the current trading day
     * @param avgVolume the calculated historical average volume
     * @return true if the current volume is at least double the average, false otherwise
     */
    private boolean evaluateVolumeSpike(BigDecimal currentVolumne, BigDecimal avgVolume){
        return currentVolumne.compareTo(avgVolume.multiply(new BigDecimal(2))) >= 0;
    }

    /**
     * Checks if the current volume is at least double the average.
     * @param companySymbol the unique ticker symbol of the company (e.g., "DAX")
     * @param today the reference date from which the calculation starts
     * @return true if the current volume is at least double the average, false otherwise
     */
    public boolean volumeSpikeAlert(String companySymbol, LocalDate today){
        List<BigDecimal> historicalVolumeData = getVolumeDataIncludingToday(findCompanyBySymbol(companySymbol), today);
        BigDecimal currentVolume = historicalVolumeData.remove(0);
        BigDecimal avgVolume = averageVolume(historicalVolumeData);
        
        return evaluateVolumeSpike(currentVolume, avgVolume);
    } 

    public boolean volumeSpikeAlert(String companySymbol){
        LocalDate today = LocalDate.now();
        return volumeSpikeAlert(companySymbol, today);
    }

    private DimCompany findCompanyBySymbol(String companySymbol){
        //return dimCompanyRepository.findBySymbol(companySymbol).orElseThrow(() -> new EntityNotFoundException("Company not Found!"));
        /**
        try (var conn = dataSource.getConnection();
            var stmt = conn.createStatement();
            var rs = stmt.executeQuery("SELECT pg_postmaster_start_time() AS startzeit, datid FROM pg_stat_database WHERE datname = current_database()")) {
            if (rs.next()) {
                System.out.println(">>> JAVA DB STARTZEIT: " + rs.getTimestamp("startzeit") + " | DATID: " + rs.getLong("datid"));
            }
        } catch (Exception e) {
            System.err.println("DB Info Fehler: " + e.getMessage());
        } **/
        DimCompany company = dimCompanyRepository.findBySymbol(companySymbol).orElseThrow(() -> new EntityNotFoundException("Company not Found!"));
        
        return company;
    }

    public List<RollingMetricDTO> findRollingMetricsByCompanyIdAndDateRange(Long companyId, LocalDate startDate, LocalDate endDate){
       List<RollingMetricProjection> rollingMetricProjections = factPricesRepository.findRollingMetricsByCompanyIdAndDateRange(companyId, startDate, endDate);
       return convertRollingMetricProjectionToDTO(rollingMetricProjections);    
    }

    public List<RollingMetricDTO> findRollingMetricsByCompanyIdAndDateRange(Long companyId){
        LocalDate today = LocalDate.now();   
        return findRollingMetricsByCompanyIdAndDateRange(companyId, today.minusDays(30), today);
    }

    private List<RollingMetricDTO> convertRollingMetricProjectionToDTO(List<RollingMetricProjection> rollingMetricProjections){
        List<RollingMetricDTO> rollingMetricDTOs = new ArrayList<>();

        for (RollingMetricProjection rollingMetricProjection : rollingMetricProjections) {
            rollingMetricDTOs.add(new RollingMetricDTO(
                                        rollingMetricProjection.getCompanyId(), 
                                        rollingMetricProjection.getName(), 
                                        rollingMetricProjection.getFullDate(), 
                                        rollingMetricProjection.getClosePrice(),
                                        rollingMetricProjection.getAvgForMe()));
        }
        return rollingMetricDTOs;
    }
}
