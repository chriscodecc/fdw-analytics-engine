package com.chriscodecc.fdw_analytics_engine.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

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
public record RollingMetricDTO(
        LocalDate priceDate,
        BigDecimal closePrice,
        BigDecimal rollingAvg30
) {} 
