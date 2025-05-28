package com.risk.server.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "financial_statement")
public class FinancialStatement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate period;      // отчётная дата
    private String  currency;      // ISO-код
    private String  source;        // “xls”, “csv”, …

    /*--- getters / setters ---*/
    public Long getId()             { return id; }
    public void setId(Long id)      { this.id = id; }

    public LocalDate getPeriod()    { return period; }
    public void setPeriod(LocalDate p) { this.period = p; }

    public String getCurrency()     { return currency; }
    public void setCurrency(String c) { this.currency = c; }

    public String getSource()       { return source; }
    public void setSource(String s) { this.source = s; }
}
