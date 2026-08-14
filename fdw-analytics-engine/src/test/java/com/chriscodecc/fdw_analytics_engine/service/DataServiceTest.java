package com.chriscodecc.fdw_analytics_engine.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import com.chriscodecc.fdw_analytics_engine.dto.CompanyDataDTO;
import com.chriscodecc.fdw_analytics_engine.model.DimCompany;
import com.chriscodecc.fdw_analytics_engine.model.DimDate;
import com.chriscodecc.fdw_analytics_engine.model.FactPrices;
import com.chriscodecc.fdw_analytics_engine.repository.DimCompanyRepository;
import com.chriscodecc.fdw_analytics_engine.repository.FactPricesRepository;

import jakarta.persistence.EntityNotFoundException;

@ExtendWith(MockitoExtension.class)
public class DataServiceTest {

    DataService dataService;
    private final FactPricesRepository factPricesRepository = mock(FactPricesRepository.class);
    private final DimCompanyRepository dimCompanyRepository = mock(DimCompanyRepository.class);
    
    DimCompany dimCompany;
    List<FactPrices> pricesList;

    int days;
    Integer companyId;
    String symbol;

    DimDate date;
    LocalDate todayDate;
    FactPrices priceRecord2;

    @BeforeEach
    void setUp(){
        dataService = new DataService(factPricesRepository, dimCompanyRepository);

        symbol = "AAPL";
        days = 7;
        companyId = 1;
        todayDate = LocalDate.of(2026, 12, 24);

        dimCompany = new DimCompany();
        dimCompany.setId(companyId);
        dimCompany.setName("Apple Inc.");
        dimCompany.setSymbol(symbol);
        dimCompany.setCountry("USA");
        dimCompany.setIndustry("Technology");

        date = new DimDate();
        date.setDayToday((short) todayDate.getDayOfMonth());
        date.setMonthToday((short) todayDate.getMonthValue());
        date.setYearToday((short) todayDate.getYear());

        FactPrices priceRecord1 = new FactPrices();
        priceRecord1.setDimCompany(dimCompany);
        priceRecord1.setDimDate(date);
        priceRecord1.setClosePrice(new BigDecimal("180.50"));
        priceRecord1.setHighPrice(new BigDecimal("182.00"));
        priceRecord1.setLowPrice(new BigDecimal("179.00"));
        priceRecord1.setOpenPrice(new BigDecimal("179.50"));
        priceRecord1.setVolume(1500000L);

        priceRecord2 = new FactPrices();
        priceRecord2.setDimCompany(dimCompany);
        priceRecord2.setDimDate(date);
        priceRecord2.setClosePrice(new BigDecimal("181.50"));
        priceRecord2.setHighPrice(new BigDecimal("181.00"));
        priceRecord2.setLowPrice(new BigDecimal("117.00"));
        priceRecord2.setOpenPrice(new BigDecimal("180.50"));
        priceRecord2.setVolume(1500200L);

        FactPrices priceRecord3 = new FactPrices();
        priceRecord3.setDimCompany(dimCompany);
        priceRecord3.setDimDate(date);
        priceRecord3.setClosePrice(new BigDecimal("186.50"));
        priceRecord3.setHighPrice(new BigDecimal("189.00"));
        priceRecord3.setLowPrice(new BigDecimal("179.00"));
        priceRecord3.setOpenPrice(new BigDecimal("185.50"));
        priceRecord3.setVolume(1490000L);

        FactPrices priceRecord4 = new FactPrices();
        priceRecord4.setDimCompany(dimCompany);
        priceRecord4.setDimDate(date);
        priceRecord4.setClosePrice(new BigDecimal("187.50"));
        priceRecord4.setHighPrice(new BigDecimal("190.00"));
        priceRecord4.setLowPrice(new BigDecimal("185.00"));
        priceRecord4.setOpenPrice(new BigDecimal("185.50"));
        priceRecord4.setVolume(1510010L);

        pricesList = new ArrayList<>();
        pricesList.add(priceRecord1);
        pricesList.add(priceRecord2);
        pricesList.add(priceRecord3);
        pricesList.add(priceRecord4);
    }

    @Test
    @DisplayName("Provide Company Data: Should return correctly mapped DTO when symbol and range are valid")
    void provideCompanyData_withValidSymbol_shouldReturnMappedDTO(){
        when(dimCompanyRepository.findBySymbol("AAPL")).thenReturn(Optional.of(dimCompany));
        when(factPricesRepository.findLatestPricesForLastPastDays(companyId, todayDate, todayDate.minusDays(days))).thenReturn(pricesList);
        
        CompanyDataDTO companyDataDTO = dataService.provideCompanyData("AAPL", todayDate, days);

        assertEquals(priceRecord2.getClosePrice(), companyDataDTO.getCompanyDataList().get(1).getClosePrice());
        assertEquals("Apple Inc.", companyDataDTO.getName());
    }

    @Test
    @DisplayName("Provide Company Data: Should throw IllegalArgumentException when days range is zero")
    void provideCompanyData_withZeroDays_shouldThrowIllegalArgumentException(){
        assertThrows(IllegalArgumentException.class, () -> {
            dataService.provideCompanyData("AAPL", todayDate, 0);
        });   
    }

    @Test
    @DisplayName("Provide Company Data: Should throw IllegalArgumentException when no historical price data is found")
    void provideCompanyData_withNoPricesData_shouldThrowIllegalArgumentException(){
        List<FactPrices> emptyList = new ArrayList<>();
        when(dimCompanyRepository.findBySymbol("AAPL")).thenReturn(Optional.of(dimCompany));
        when(factPricesRepository.findLatestPricesForLastPastDays(companyId, todayDate, todayDate.minusDays(days))).thenReturn(emptyList);
        
        assertThrows(IllegalArgumentException.class, () -> {
            dataService.provideCompanyData("AAPL", todayDate, days);
        });
    }

    @Test
    @DisplayName("Provide Company Data: Should throw EntityNotFoundException when company symbol is invalid")
    void provideCompanyData_withWrongCompanySymbol_shouldThrowEntityNotFoundException(){
        when(dimCompanyRepository.findBySymbol("WRONG")).thenReturn(Optional.empty());
        
        assertThrows(EntityNotFoundException.class, () -> {
            dataService.provideCompanyData("WRONG", todayDate, days);
        });
    }
}