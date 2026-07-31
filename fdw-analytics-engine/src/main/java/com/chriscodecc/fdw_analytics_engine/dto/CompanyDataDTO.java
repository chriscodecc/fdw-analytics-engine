package com.chriscodecc.fdw_analytics_engine.dto;

import java.math.BigDecimal;
import java.util.List;

import org.antlr.v4.runtime.misc.NotNull;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CompanyDataDTO {
    
    String name;
    String symbol;
    String country;
    String industry;

    List<CompanyData> companyDataList;

    public CompanyDataDTO(){}

    @Getter
    @Setter
    public static class CompanyData {
    
    private Short dayToday;
    private Short monthToday;
    private Short yearToday;

    private BigDecimal closePrice;
    private BigDecimal highPrice;
    private BigDecimal lowPrice;
    private BigDecimal openPrice;
    private Long volume;

    public CompanyData(Short dayToday, Short monthToday, Short yearToday,
        BigDecimal closePirce, BigDecimal highPrice, BigDecimal lowPirce, BigDecimal openPirce, Long volume){
            this.dayToday = dayToday;
            this.monthToday = monthToday;
            this.yearToday = yearToday;
            this.closePrice = closePirce;
            this.highPrice = highPrice;
            this.lowPrice = lowPirce;
            this.openPrice = openPirce;
            this.volume = volume;
        }
    }
}
