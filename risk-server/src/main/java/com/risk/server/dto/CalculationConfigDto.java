package com.risk.server.dto;

/**
 * DTO для передачи настроек VaR между сервером и клиентом.
 */
public record CalculationConfigDto(
        String method,            // HISTORICAL, PARAMETRIC или MONTE_CARLO
        double confidenceLevel,   // 0.80–0.99
        int horizonDays           // горизонт (дней)
) {}
