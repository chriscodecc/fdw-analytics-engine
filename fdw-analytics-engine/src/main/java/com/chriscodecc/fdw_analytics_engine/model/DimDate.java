package com.chriscodecc.fdw_analytics_engine.model;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Entity
public class DimDate {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "date_id")
    private Integer id;

    @Column(name = "full_date")
    private LocalDate fullDate;

    @Column(name = "day")
    private Short dayToday;

    @Column(name = "month")
    private Short monthToday;

    @Column(name = "year")
    private Short yearToday;
}
