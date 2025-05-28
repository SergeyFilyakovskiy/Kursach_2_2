package com.risk.server.model;

import jakarta.persistence.*;

@Entity
@Table(name = "calculation_config")
public class CalculationConfig {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** HISTORICAL, PARAMETRIC, MONTE_CARLO */
    @Column(nullable = false)
    private String method;

    /** уровень доверия, например 0.95 */
    @Column(nullable = false)
    private double confidenceLevel;

    /** горизонт риска в днях */
    @Column(nullable = false)
    private int horizonDays;

    public CalculationConfig() {}

    public CalculationConfig(String method, double confidenceLevel, int horizonDays) {
        this.method = method;
        this.confidenceLevel = confidenceLevel;
        this.horizonDays = horizonDays;
    }

    public Long   getId()              { return id; }
    public String getMethod()          { return method; }
    public double getConfidenceLevel() { return confidenceLevel; }
    public int    getHorizonDays()     { return horizonDays; }

    public void setMethod(String method)           { this.method = method; }
    public void setConfidenceLevel(double lvl)     { this.confidenceLevel = lvl; }
    public void setHorizonDays(int horizonDays)    { this.horizonDays = horizonDays; }
}
