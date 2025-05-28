// risk-server/src/main/java/com/risk/server/model/ValidationError.java
package com.risk.server.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "data_validation_error")
@Getter
@Setter
@NoArgsConstructor
public class ValidationError {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String symbol;

    /** дата как строка "yyyy-MM-dd" */
    private String date;

    private String field;
    private String message;

    /** Удобный конструктор без id */
    public ValidationError(String symbol, String date, String field, String message) {
        this.symbol = symbol;
        this.date    = date;
        this.field   = field;
        this.message = message;
    }
}
