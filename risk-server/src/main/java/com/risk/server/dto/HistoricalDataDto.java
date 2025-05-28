// src/main/java/com/risk/server/dto/HistoricalDataDto.java
package com.risk.server.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;

/** DTO для отдачи клиенту только нужных полей */
@JsonIgnoreProperties(ignoreUnknown = true)
public record HistoricalDataDto(
        long id,
        String symbol,
        String date,
        BigDecimal price,
        BigDecimal ret
) {}
