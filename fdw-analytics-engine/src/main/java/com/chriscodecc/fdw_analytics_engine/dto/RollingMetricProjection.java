package com.chriscodecc.fdw_analytics_engine.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface RollingMetricProjection {
    Long getCompanyId();
    String getName();
    LocalDate getFullDate();
    BigDecimal getClosePrice();
    BigDecimal getAvgForMe();
}
