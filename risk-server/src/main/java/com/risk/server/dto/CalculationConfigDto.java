package com.risk.server.dto;

// DTO для REST<->клиент
public record CalculationConfigDto(
        Long id,
        String method,
        Double confidenceLevel,
        Integer horizonDays
) {}
