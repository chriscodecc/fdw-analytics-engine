package com.chriscodecc.fdw_analytics_engine.model;

import java.math.BigDecimal;

/**
 * Immutable carrier holding unnormalized market metrics and baseline indicator prices
 * for a single evaluation date.
 *
 * @param todaysClosingPrice the reference closing price of the current trading day
 * @param sma the simple moving average benchmark price
 * @param rollingAvg the historical rolling average benchmark price
 * @param dailyReturn the raw percentage price return of the day
 * @param volumeSpike the detected trading volume anomaly factor
 */
public record RawMarketMetric( 
    BigDecimal todaysClosingPrice,
    BigDecimal sma,
    BigDecimal rollingAvg,
    BigDecimal dailyReturn,
    BigDecimal volumeSpike
){}
