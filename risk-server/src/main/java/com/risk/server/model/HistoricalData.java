// risk-server/src/main/java/com/risk/server/model/HistoricalData.java
package com.risk.server.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Getter
@Setter
@Table(name = "historical_data")
public class HistoricalData {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String symbol;

    @Column(name = "price_date")
    private LocalDate date;

    private BigDecimal price;

    private BigDecimal ret; // доходность, если нужна

}
