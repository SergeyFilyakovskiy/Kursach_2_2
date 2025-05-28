package com.risk.server.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity @Table(name="risk_position")
@Getter
@Setter                       // Lombok
public class RiskPosition
{
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String  symbol;
    private LocalDate valDate;
    private int     quantity;
    private BigDecimal lastPrice;

}
