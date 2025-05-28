// src/main/java/com/risk/server/dto/ValidationErrorDto.java
package com.risk.server.dto;

public record ValidationErrorDto(
        Long id,
        String symbol,
        String date,
        String field,
        String message
) {}
