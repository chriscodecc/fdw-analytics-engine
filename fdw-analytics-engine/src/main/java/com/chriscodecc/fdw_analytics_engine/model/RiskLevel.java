package com.chriscodecc.fdw_analytics_engine.model;

import java.util.HashMap;
import java.util.Map;

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
    
    public int getScore() {
        return score;
    }

    public static RiskLevel fromScore(int score){
        RiskLevel result = BY_SCORE.get(score);
        if(result == null){
            throw new IllegalArgumentException("Unknown risk score: " + score + "\n Score should be between 1 (LOW) and 4 (CRITICAL).");
        }
        return result;
    }

    public boolean isMoreSevereThan(RiskLevel other){
        return this.score > other.score;
    }

}
