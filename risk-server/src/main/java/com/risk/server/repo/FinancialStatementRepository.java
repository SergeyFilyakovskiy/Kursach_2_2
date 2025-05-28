package com.risk.server.repo;

import com.risk.server.model.FinancialStatement;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FinancialStatementRepository
        extends JpaRepository<FinancialStatement, Long> { }
