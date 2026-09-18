package com.chriscodecc.fdw_analytics_engine.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter 
@Setter 
@NoArgsConstructor
@AllArgsConstructor
public class FactPricesDTO {
    private Integer price_id;
    private Integer company_id;
    private Integer date_id;
    private LocalDate fullDate;
    private BigDecimal closePrice;
    private BigDecimal highPrice;
    private BigDecimal lowPrice;
    private BigDecimal openPrice;
    private Long volume;
    private BigDecimal avgVolume;

    
}
