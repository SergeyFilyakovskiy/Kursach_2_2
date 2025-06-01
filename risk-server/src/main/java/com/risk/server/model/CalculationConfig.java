package com.risk.server.model;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "calculation_config")
public class CalculationConfig {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Допустимые значения: HISTORICAL, PARAMETRIC, MONTE_CARLO
    @Column(nullable = false, length = 32)
    private String method;

    // уровень доверия, 0.80 … 0.99
    @Column(nullable = false)
    private Double confidenceLevel;

    // горизонт в днях
    @Column(nullable = false)
    private Integer horizonDays;

    // === конструкторы, геттеры/сеттеры ===
    public CalculationConfig() {}

    public CalculationConfig(String method, Double confidenceLevel, Integer horizonDays) {
        this.method = method;
        this.confidenceLevel = confidenceLevel;
        this.horizonDays = horizonDays;
    }

    public Long getId() {
        return id;
    }
    public void setId(Long id) {
        this.id = id;
    }

    public String getMethod() {
        return method;
    }
    public void setMethod(String method) {
        this.method = method;
    }

    public Double getConfidenceLevel() {
        return confidenceLevel;
    }
    public void setConfidenceLevel(Double confidenceLevel) {
        this.confidenceLevel = confidenceLevel;
    }

    public Integer getHorizonDays() {
        return horizonDays;
    }
    public void setHorizonDays(Integer horizonDays) {
        this.horizonDays = horizonDays;
    }
}
