package com.chriscodecc.fdw_analytics_engine.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.chriscodecc.fdw_analytics_engine.dto.CompanyDataDTO;
import com.chriscodecc.fdw_analytics_engine.dto.CompanyDataDTO.CompanyData;
import com.chriscodecc.fdw_analytics_engine.model.FactPrices;
import com.chriscodecc.fdw_analytics_engine.repository.DimCompanyRepository;
import com.chriscodecc.fdw_analytics_engine.repository.FactPricesRepository;

import jakarta.persistence.EntityNotFoundException;

@Service
public class DataService {
    private final DimCompanyRepository dimCompanyRepository;
    private final FactPricesRepository factPricesRepository;

    public DataService(FactPricesRepository factPricesRepository, DimCompanyRepository dimCompanyRepository){
        this.factPricesRepository = factPricesRepository;
        this.dimCompanyRepository = dimCompanyRepository;
    }

    /**
     * Provides historical company price data for a specified number of days.
     * 
     * @param companySymbol the unique ticker symbol of the company (e.g., "DAX")
     * @param today the reference date from which the historical calculation starts
     * @param days the number of days the data will be provided for
     * @return a data transfer object containing compiled company metadata and historical price records
     * @throws IllegalArgumentException if the number of days is zero or negative
     * @throws IllegalArgumentException if no historical fact prices are available for the specified period
     */
    @Transactional
    public CompanyDataDTO provideCompanyData(String companySymbol, LocalDate today, int days){
        if(days <= 0){
            throw new IllegalArgumentException("Days have to be greater then 0.");
        }

        CompanyDataDTO companyDataDTO = new CompanyDataDTO();
        Integer companyId = getCompanyId(companySymbol);
        List<FactPrices> factPrices = getFactPricesForPastDays(companyId, today, days);
        
        if(factPrices.isEmpty()){
            throw new IllegalArgumentException("No FactPrices Data avileble for " + companySymbol);
        }
        
        companyDataDTO.setName(factPrices.get(0).getDimCompany().getName());
        companyDataDTO.setSymbol(factPrices.get(0).getDimCompany().getSymbol());
        companyDataDTO.setCountry(factPrices.get(0).getDimCompany().getCountry());
        companyDataDTO.setIndustry(factPrices.get(0).getDimCompany().getIndustry());

        List<CompanyDataDTO.CompanyData> companyData = new ArrayList<>();
        for (int i=0; i<factPrices.size(); i++) {
            companyData.add(new CompanyData(factPrices.get(i).getDimDate().getDayToday(),
                                        factPrices.get(i).getDimDate().getMonthToday(),
                                        factPrices.get(i).getDimDate().getYearToday(),
                                        factPrices.get(i).getClosePrice(),
                                        factPrices.get(i).getHighPrice(),
                                        factPrices.get(i).getLowPrice(),
                                        factPrices.get(i).getOpenPrice(),
                                        factPrices.get(i).getVolume())
            );     
        }
        companyDataDTO.setCompanyDataList(companyData);

        return companyDataDTO;
    }

    public CompanyDataDTO provideCompanyData(String companySymbol){
        LocalDate today = LocalDate.now();
        return provideCompanyData(companySymbol, today, 7);
    }
    
    private Integer getCompanyId(String companySymbol){
        return dimCompanyRepository.findBySymbol(companySymbol).orElseThrow(() -> new EntityNotFoundException("Company with symbol '" + companySymbol + "' not found.")).getId();
    }

    private List<FactPrices> getFactPricesForPastDays(Integer compId, LocalDate today, int days){
        return factPricesRepository.findLatestPricesForLastPastDays(compId, today, today.minusDays(days));
    }
}
