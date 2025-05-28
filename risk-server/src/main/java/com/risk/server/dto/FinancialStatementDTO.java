package com.risk.server.dto;

import java.time.LocalDate;

public record FinancialStatementDTO(
        LocalDate period,
        String    currency,
        String    source) { }
