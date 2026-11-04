package com.chriscodecc.fdw_analytics_engine.model;

import java.math.BigDecimal;

/**
 * Immutable boundary definition for categorizing market indicators into risk tiers.
 * <p>
 * Enforces strict non-null values and descending threshold ordering:
 * {@code critical >= high >= normal}.
 *
 * @param critical the lower boundary for the critical risk tier
 * @param high the lower boundary for the high risk tier
 * @param normal the lower boundary for the normal risk tier
 * 
 * @throws IllegalArgumentException if any threshold is {@code null} or if boundaries
 *                                  violate the descending order constraint
 */
public record RiskThresholds(
    BigDecimal critical,
    BigDecimal high,
    BigDecimal normal
) {
    public RiskThresholds {
        if (critical == null || high == null || normal == null) {
            throw new IllegalArgumentException("Threshold values must not be null.");
        }
        if (critical.compareTo(high) < 0 || high.compareTo(normal) < 0) {
            throw new IllegalArgumentException("Thresholds must follow: critical >= high >= normal");
        }
    }
}
