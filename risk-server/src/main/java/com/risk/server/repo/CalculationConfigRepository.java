package com.risk.server.repo;

import com.risk.server.model.CalculationConfig;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CalculationConfigRepository
        extends JpaRepository<CalculationConfig, Long> {
    // всё остальное достаёт JpaRepository
    CalculationConfig findFirstByOrderByIdAsc();
}
