package com.chriscodecc.fdw_analytics_engine.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
public class FactPrices {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "price_id")
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "date_id")
    private DimDate dimDate;

    @ManyToOne
    @JoinColumn(name = "company_id")
    private DimCompany dimCompany;

    @Column(name = "close_price")
    private BigDecimal closePrice;

    @Column(name = "high_price")
    private BigDecimal highPrice;

    @Column(name = "low_price")
    private BigDecimal lowPrice;

    @Column(name = "open_price")
    private BigDecimal openPrice;

    @Column(name = "volume")
    private Long volume;

}
