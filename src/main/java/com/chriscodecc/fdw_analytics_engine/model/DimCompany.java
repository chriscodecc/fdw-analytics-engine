package com.chriscodecc.fdw_analytics_engine.model;

import java.math.BigInteger;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * JPA entity representing the company dimension table in the star schema.
 * <p>
 * Stores corporate master data and classification attributes including ticker symbols,
 * company name, country of origin, and industry sector to support analytical segmentation.
 */
@Getter
@Setter
@Entity
@Table(name="dim_company")
public class DimCompany {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "company_id")
    private Integer id;

    @Column(name = "name")
    private String name;

    @Column(name = "symbol")
    private String symbol;

    @Column(name= "country")
    private String country;

    @Column(name= "industry")
    private String industry;
    
    
}
