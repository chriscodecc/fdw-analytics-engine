package com.chriscodecc.fdw_analytics_engine.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.lang.foreign.Linker.Option;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.swing.text.html.parser.Entity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.support.incrementer.Db2MainframeMaxValueIncrementer;

import com.chriscodecc.fdw_analytics_engine.model.DimCompany;
import com.chriscodecc.fdw_analytics_engine.model.DimDate;
import com.chriscodecc.fdw_analytics_engine.model.FactPrices;
import com.chriscodecc.fdw_analytics_engine.repository.DimCompanyRepository;
import com.chriscodecc.fdw_analytics_engine.repository.DimDateRepository;
import com.chriscodecc.fdw_analytics_engine.repository.FactPricesRepository;

import jakarta.persistence.EntityNotFoundException;

@ExtendWith(MockitoExtension.class)
public class AnalyticsServiceTest {
    
    @Mock FactPricesRepository factPricesRepository;
    @Mock DimCompanyRepository dimCompanyRepository;
    @Mock DimDateRepository dimDateRepository;

    @InjectMocks
    AnalyticsService analyticsService;

    DimCompany dimCompanyDAX;
    List<FactPrices> latestPrices;
    List<FactPrices> latestPricesSpike;
    List<FactPrices> latestPricesOneElement;
    List<FactPrices> historicalFactPrices;
    
    FactPrices daxToday;
    FactPrices daxYesterday;
    FactPrices daxSpike;
    FactPrices daxDay1Belove;
    FactPrices daxDay1Above;
    FactPrices daxDay1DoubleVolume;
    FactPrices daxDay1LowerVolume;

    LocalDate todayDate;
    LocalDate yesterdayDate;
    LocalDate dateDay3; 
    LocalDate dateDay4; 
    LocalDate dateDay5; 
    LocalDate dateDay6; 
    LocalDate dateDay7;

    @BeforeEach
    void setUp(){

        todayDate = LocalDate.of(2026, 12, 24);
        yesterdayDate = todayDate.minusDays(1);
        dateDay3 = yesterdayDate.minusDays(1); 
        dateDay4 = yesterdayDate.minusDays(2); 
        dateDay5 = yesterdayDate.minusDays(5); 
        dateDay6 = yesterdayDate.minusDays(6); 
        dateDay7 = yesterdayDate.minusDays(7);

        dimCompanyDAX = new DimCompany();
        dimCompanyDAX.setName("DAX COMPANYS");
        dimCompanyDAX.setSymbol("DAX");
        dimCompanyDAX.setCountry("Germany");
        dimCompanyDAX.setIndustry("all of them");
        dimCompanyDAX.setId(1);

        DimDate dimDateToday = new DimDate();
        dimDateToday.setFullDate(todayDate);
        dimDateToday.setDayToday((short) todayDate.getDayOfMonth());
        dimDateToday.setMonthToday((short) todayDate.getMonthValue());
        dimDateToday.setYearToday((short) todayDate.getYear());

        DimDate dimDateYesterday = new DimDate();
        dimDateYesterday.setFullDate(yesterdayDate);
        dimDateYesterday.setDayToday((short) yesterdayDate.getDayOfMonth());
        dimDateYesterday.setMonthToday((short) yesterdayDate.getMonthValue());
        dimDateYesterday.setYearToday((short) yesterdayDate.getYear());
       
        daxToday = new FactPrices();
        daxToday.setDimCompany(dimCompanyDAX); 
        daxToday.setDimDate(dimDateToday);    
        daxToday.setOpenPrice(new BigDecimal("18450.00"));
        daxToday.setHighPrice(new BigDecimal("18550.50"));
        daxToday.setLowPrice(new BigDecimal("18400.20"));
        daxToday.setClosePrice(new BigDecimal("18500.00"));
        daxToday.setVolume(1250000L); 

        daxYesterday = new FactPrices();
        daxYesterday.setDimCompany(dimCompanyDAX); 
        daxYesterday.setDimDate(dimDateYesterday); 
        daxYesterday.setOpenPrice(new BigDecimal("18380.00"));
        daxYesterday.setHighPrice(new BigDecimal("18460.00"));
        daxYesterday.setLowPrice(new BigDecimal("18350.10"));
        daxYesterday.setClosePrice(new BigDecimal("18420.50"));
        daxYesterday.setVolume(1100000L);

        daxDay1Belove = new FactPrices();
        daxDay1Belove.setDimCompany(dimCompanyDAX);
        daxDay1Belove.setDimDate(dimDateToday);
        daxDay1Belove.setOpenPrice(new BigDecimal("18400.00"));
        daxDay1Belove.setHighPrice(new BigDecimal("18450.00"));
        daxDay1Belove.setLowPrice(new BigDecimal("18320.00"));
        daxDay1Belove.setClosePrice(new BigDecimal("18386.00"));
        daxDay1Belove.setVolume(1150000L);

        daxDay1Above = new FactPrices();
        daxDay1Above.setDimCompany(dimCompanyDAX);
        daxDay1Above.setDimDate(dimDateToday);
        daxDay1Above.setOpenPrice(new BigDecimal("18400.00"));
        daxDay1Above.setHighPrice(new BigDecimal("18450.00"));
        daxDay1Above.setLowPrice(new BigDecimal("18520.00"));
        daxDay1Above.setClosePrice(new BigDecimal("18386.00"));
        daxDay1Above.setVolume(1150000L);

        daxDay1DoubleVolume = new FactPrices();
        daxDay1DoubleVolume.setDimCompany(dimCompanyDAX);
        daxDay1DoubleVolume.setDimDate(dimDateToday);
        daxDay1DoubleVolume.setOpenPrice(new BigDecimal("18400.00"));
        daxDay1DoubleVolume.setHighPrice(new BigDecimal("18450.00"));
        daxDay1DoubleVolume.setLowPrice(new BigDecimal("18320.00"));
        daxDay1DoubleVolume.setClosePrice(new BigDecimal("18386.00"));
        daxDay1DoubleVolume.setVolume(2520000L); 

        daxDay1LowerVolume = new FactPrices();
        daxDay1LowerVolume.setDimCompany(dimCompanyDAX);
        daxDay1LowerVolume.setDimDate(dimDateToday);
        daxDay1LowerVolume.setOpenPrice(new BigDecimal("18400.00"));
        daxDay1LowerVolume.setHighPrice(new BigDecimal("18450.00"));
        daxDay1LowerVolume.setLowPrice(new BigDecimal("18320.00"));
        daxDay1LowerVolume.setClosePrice(new BigDecimal("18386.00"));
        daxDay1LowerVolume.setVolume(1000000L); 


        FactPrices daxDay2 = new FactPrices();
        daxDay2.setDimCompany(dimCompanyDAX);
        daxDay2.setDimDate(dimDateYesterday);
        daxDay2.setOpenPrice(new BigDecimal("18400.00"));
        daxDay2.setHighPrice(new BigDecimal("18450.00"));
        daxDay2.setLowPrice(new BigDecimal("18320.00"));
        daxDay2.setClosePrice(new BigDecimal("18390.00"));
        daxDay2.setVolume(1150000L);

        DimDate dimDateDay3 = new DimDate();
        dimDateDay3.setFullDate(dateDay3);
        dimDateDay3.setDayToday((short) dateDay3.getDayOfMonth());
        dimDateDay3.setMonthToday((short) dateDay3.getMonthValue());
        dimDateDay3.setYearToday((short) dateDay3.getYear());

        FactPrices daxDay3 = new FactPrices();
        daxDay3.setDimCompany(dimCompanyDAX);
        daxDay3.setDimDate(dimDateDay3);
        daxDay3.setOpenPrice(new BigDecimal("18400.00"));
        daxDay3.setHighPrice(new BigDecimal("18450.00"));
        daxDay3.setLowPrice(new BigDecimal("18320.00"));
        daxDay3.setClosePrice(new BigDecimal("18390.00"));
        daxDay3.setVolume(1150000L);

        DimDate dimDateDay4 = new DimDate();
        dimDateDay4.setFullDate(dateDay4);
        dimDateDay4.setDayToday((short) dateDay4.getDayOfMonth());
        dimDateDay4.setMonthToday((short) dateDay4.getMonthValue());
        dimDateDay4.setYearToday((short) dateDay4.getYear());

        FactPrices daxDay4 = new FactPrices();
        daxDay4.setDimCompany(dimCompanyDAX);
        daxDay4.setDimDate(dimDateDay4);
        daxDay4.setOpenPrice(new BigDecimal("18420.00"));
        daxDay4.setHighPrice(new BigDecimal("18460.00"));
        daxDay4.setLowPrice(new BigDecimal("18380.00"));
        daxDay4.setClosePrice(new BigDecimal("18410.00"));
        daxDay4.setVolume(1200000L);

        DimDate dimDateDay5 = new DimDate();
        dimDateDay5.setFullDate(dateDay5);
        dimDateDay5.setDayToday((short) dateDay5.getDayOfMonth());
        dimDateDay5.setMonthToday((short) dateDay5.getMonthValue());
        dimDateDay5.setYearToday((short) dateDay5.getYear());

        FactPrices daxDay5 = new FactPrices();
        daxDay5.setDimCompany(dimCompanyDAX);
        daxDay5.setDimDate(dimDateDay5);
        daxDay5.setOpenPrice(new BigDecimal("18320.00"));
        daxDay5.setHighPrice(new BigDecimal("18380.00"));
        daxDay5.setLowPrice(new BigDecimal("18280.00"));
        daxDay5.setClosePrice(new BigDecimal("18350.00"));
        daxDay5.setVolume(1050000L);

        DimDate dimDateDay6 = new DimDate();
        dimDateDay6.setFullDate(dateDay6);
        dimDateDay6.setDayToday((short) dateDay6.getDayOfMonth());
        dimDateDay6.setMonthToday((short) dateDay6.getMonthValue());
        dimDateDay6.setYearToday((short) dateDay6.getYear());

        FactPrices daxDay6 = new FactPrices();
        daxDay6.setDimCompany(dimCompanyDAX);
        daxDay6.setDimDate(dimDateDay6);
        daxDay6.setOpenPrice(new BigDecimal("18250.00"));
        daxDay6.setHighPrice(new BigDecimal("18310.00"));
        daxDay6.setLowPrice(new BigDecimal("18200.00"));
        daxDay6.setClosePrice(new BigDecimal("18290.00"));
        daxDay6.setVolume(1300000L);

        DimDate dimDateDay7 = new DimDate();
        dimDateDay7.setFullDate(dateDay7);
        dimDateDay7.setDayToday((short) dateDay7.getDayOfMonth());
        dimDateDay7.setMonthToday((short) dateDay7.getMonthValue());
        dimDateDay7.setYearToday((short) dateDay7.getYear());

        FactPrices daxDay7 = new FactPrices();
        daxDay7.setDimCompany(dimCompanyDAX);
        daxDay7.setDimDate(dimDateDay7);
        daxDay7.setOpenPrice(new BigDecimal("18200.00"));
        daxDay7.setHighPrice(new BigDecimal("18260.00"));
        daxDay7.setLowPrice(new BigDecimal("18150.00"));
        daxDay7.setClosePrice(new BigDecimal("18220.00"));
        daxDay7.setVolume(1180000L);

        daxSpike = new FactPrices();
        daxSpike.setDimCompany(dimCompanyDAX); 
        daxSpike.setDimDate(dimDateYesterday); 
        daxSpike.setOpenPrice(new BigDecimal("18380.00"));
        daxSpike.setHighPrice(new BigDecimal("18460.00"));
        daxSpike.setLowPrice(new BigDecimal("18350.10"));
        daxSpike.setClosePrice(new BigDecimal("32420.50"));
        daxSpike.setVolume(1100000L);

        latestPrices = new ArrayList<FactPrices>();
        latestPrices.add(daxToday);
        latestPrices.add(daxYesterday);

        latestPricesSpike = new ArrayList<FactPrices>();
        latestPricesSpike.add(daxToday);
        latestPricesSpike.add(daxSpike);

        latestPricesOneElement = new ArrayList<FactPrices>();
        latestPricesOneElement.add(daxToday);

        historicalFactPrices = new ArrayList<>();
        historicalFactPrices.add(daxDay1Belove);
        historicalFactPrices.add(daxDay2);
        historicalFactPrices.add(daxDay3);
        historicalFactPrices.add(daxDay4);
        historicalFactPrices.add(daxDay5);
        historicalFactPrices.add(daxDay6);
        historicalFactPrices.add(daxDay7);
    }

    //#region DAILY RETURN
    @Test
    void checkDailyReturnThresholdShouldReturnFalse(){
        when(dimCompanyRepository.findBySymbol("DAX")).thenReturn(Optional.of(dimCompanyDAX));
        when(factPricesRepository.findLatestPriceBeforeDate(dimCompanyDAX.getId(), todayDate)).thenReturn(latestPrices);

        assertEquals(false, analyticsService.checkDailyReturnThreshold("DAX", todayDate));
    }

    @Test
    void checkDailyReturnThresholdShouldReturnTrue(){
        when(dimCompanyRepository.findBySymbol("DAX")).thenReturn(Optional.of(dimCompanyDAX));
        when(factPricesRepository.findLatestPriceBeforeDate(dimCompanyDAX.getId(), todayDate)).thenReturn(latestPricesSpike);

        assertEquals(true, analyticsService.checkDailyReturnThreshold("DAX", todayDate));
    }

    @Test
    void checkDailyReturnThresholdShouldThrowEntityNotFoundExc(){
        when(dimCompanyRepository.findBySymbol("WRONG")).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> {
            analyticsService.checkDailyReturnThreshold("WRONG");
        });
    }

    @Test
    void checkDailyReturnThresholdShouldThrowDataIntegrityViolationException(){
        when(dimCompanyRepository.findBySymbol("DAX")).thenReturn(Optional.of(dimCompanyDAX));
        when(factPricesRepository.findLatestPriceBeforeDate(dimCompanyDAX.getId(), todayDate)).thenReturn(latestPricesOneElement);

        assertThrows(DataIntegrityViolationException.class, () -> {
            analyticsService.checkDailyReturnThreshold("DAX", todayDate);
        });
    }

    //#region SMA

    @Test
    void smaDropsBelowThresholdShouldReturnTrue(){
        when(dimCompanyRepository.findBySymbol("DAX")).thenReturn(Optional.of(dimCompanyDAX));
        when(factPricesRepository.findLatestPricesForLastPastDays(dimCompanyDAX.getId(),todayDate, todayDate.minusDays(8))).thenReturn(historicalFactPrices);
        when(factPricesRepository.findFirstByDimCompanyIdOrderByDimDateFullDateDesc(dimCompanyDAX.getId())).thenReturn(Optional.of(daxDay1Belove));

        assertEquals(true, analyticsService.simpleMovingAverageAlert("DAX", todayDate));
    }

    @Test
    void smaStaysAboveThresholdShouldReturnFalse(){
        when(dimCompanyRepository.findBySymbol("DAX")).thenReturn(Optional.of(dimCompanyDAX));
        when(factPricesRepository.findLatestPricesForLastPastDays(dimCompanyDAX.getId(),todayDate, todayDate.minusDays(8))).thenReturn(historicalFactPrices);
        when(factPricesRepository.findFirstByDimCompanyIdOrderByDimDateFullDateDesc(dimCompanyDAX.getId())).thenReturn(Optional.of(daxDay1Above));

        assertEquals(false, analyticsService.simpleMovingAverageAlert("DAX", todayDate));
    }

    @Test
    void checksmaShouldReturnEntitiyNotFoundException(){
        when(dimCompanyRepository.findBySymbol("WAX")).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> {
            analyticsService.simpleMovingAverageAlert("WAX", todayDate);
        });
    }

    @Test 
    void smaNoHistoricalDataAvailableShouldThrowIllegalArgumentException(){
        List<FactPrices> emptyList = new ArrayList<>();
        when(dimCompanyRepository.findBySymbol("DAX")).thenReturn(Optional.of(dimCompanyDAX));
        when(factPricesRepository.findLatestPricesForLastPastDays(1,todayDate, todayDate.minusDays(8))).thenReturn(emptyList);
        
        assertThrows(IllegalArgumentException.class, () -> {
            analyticsService.simpleMovingAverageAlert("DAX", todayDate);
        });
    }

    @Test
    void volumeSpikeAlertShouldReturnTrue(){
        historicalFactPrices.set(0, daxDay1DoubleVolume);
        when(dimCompanyRepository.findBySymbol("DAX")).thenReturn(Optional.of(dimCompanyDAX));
        when(factPricesRepository.findLatestPricesForLastPastDays(1, todayDate, todayDate.minusDays(8))).thenReturn(historicalFactPrices);
        
        assertEquals(true, analyticsService.volumeSpikeAlert("DAX", todayDate));
    }

    @Test
    void volumeSpikeAlertShouldReturnFalse(){
        historicalFactPrices.set(0, daxDay1LowerVolume);
        when(dimCompanyRepository.findBySymbol("DAX")).thenReturn(Optional.of(dimCompanyDAX));
        when(factPricesRepository.findLatestPricesForLastPastDays(1, todayDate, todayDate.minusDays(8))).thenReturn(historicalFactPrices);
        
        assertEquals(false, analyticsService.volumeSpikeAlert("DAX", todayDate));
    }

    @Test
    void volumeSpikeAlertInvalidCompanyShouldThrowEntityNotFoundEx(){
        historicalFactPrices.set(0, daxDay1LowerVolume);
        when(dimCompanyRepository.findBySymbol("WAX")).thenReturn(Optional.empty());
        //when(factPricesRepository.findLatesPricesForLastXDays(1, todayDate, todayDate.minusDays(8))).thenReturn(historicalFactPrices);
        
        assertThrows(EntityNotFoundException.class, () -> {
            analyticsService.volumeSpikeAlert("WAX", todayDate);
        });
    }

    @Test
    void volumeSpikeAlertNoHistoricDataFoundShouldThrowIllegalArgumentEx(){
        historicalFactPrices.set(0, daxDay1LowerVolume);
        when(dimCompanyRepository.findBySymbol("DAX")).thenReturn(Optional.of(dimCompanyDAX));
        when(factPricesRepository.findLatestPricesForLastPastDays(1, todayDate, todayDate.minusDays(8))).thenReturn(new ArrayList<>());
        
        assertThrows(IllegalArgumentException.class, () -> {
            analyticsService.volumeSpikeAlert("DAX", todayDate);
        });
    }

}
