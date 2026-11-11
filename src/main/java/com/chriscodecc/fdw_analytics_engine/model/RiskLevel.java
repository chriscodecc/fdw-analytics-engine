package com.chriscodecc.fdw_analytics_engine.model;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents categorical severity tiers for financial market risk assessment.
 * <p>
 * Each level maps directly to an ordinal integer score ranging from 1 (LOW) to 4 (CRITICAL).
 */
public enum RiskLevel {
   LOW(1),
    NORMAL(2),
    HIGH(3),
    CRITICAL(4);

    private static final Map<Integer, RiskLevel> BY_SCORE = new HashMap<>();
    static {
        for(RiskLevel rl : values()){
            BY_SCORE.put(rl.score, rl);    
        }
    }
    public final Integer score;
    RiskLevel(int score) {
        this.score = score;
    }
    
    /**
     * Gets the numeric severity score associated with this risk level.
     *
     * @return the integer score (1 to 4)
     */
    public int getScore() {
        return score;
    }

    /**
     * Resolves an integer score to its corresponding {@link RiskLevel} constant.
     *
     * @param score the numerical score to resolve (1 to 4)
     * @return the matching {@link RiskLevel}
     * @throws IllegalArgumentException if the provided score does not map to any defined level
     */
    public static RiskLevel fromScore(int score){
        RiskLevel result = BY_SCORE.get(score);
        if(result == null){
            throw new IllegalArgumentException("Unknown risk score: " + score + "\n Score should be between 1 (LOW) and 4 (CRITICAL).");
        }
        return result;
    }

    /**
     * Compares the severity of this risk level against another.
     *
     * @param other the target {@link RiskLevel} to compare against
     * @return {@code true} if this level has a strictly higher severity score than {@code other}, otherwise {@code false}
     * @throws NullPointerException if {@code other} is {@code null}
     */
    public boolean isMoreSevereThan(RiskLevel other){
        if (other == null) {
            throw new NullPointerException("Target RiskLevel to compare against must not be null.");
        }
        return this.score > other.score;
    }

}
