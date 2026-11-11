package com.chriscodecc.fdw_analytics_engine.dto;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Immutable projection representing a single trading day's closing price
 * alongside its calculated 30-session Simple Moving Average (SMA).
 * 
 * @param priceDate The market session closing date (UTC).
 * @param closePrice Raw unadjusted daily closing price.
 * @param rollingAvg30 30-session rolling window average. During the initial
 *                       warm-up period (<30 entries), reflects the running average
 *                       over available preceding rows.
 */
@Getter
@Setter
public class RollingMetricDTO{
        Integer companyId;
        String name;
        LocalDate priceDate;
        BigDecimal closePrice;
        BigDecimal rollingAvg30;

        public RollingMetricDTO(Integer companyId,String name, LocalDate date, BigDecimal closePrice, BigDecimal avg){
                this.companyId = companyId;
                this.name = name;
                this.priceDate = date;
                this.closePrice = closePrice;
                this.rollingAvg30 = avg;
        }

        @Override
        public String toString() {
                return "ID: " + companyId + " Name: " + name + " PriceDate: " + priceDate + " ClosePrice: " + closePrice + " RollingAvg: " + rollingAvg30;
        }

        
}
