package com.risk.server.service;

import com.risk.server.model.CalculationConfig;
import com.risk.server.repo.CalculationConfigRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class ConfigService {
    private final CalculationConfigRepository repo;

    public ConfigService(CalculationConfigRepository repo) {
        this.repo = repo;
    }

    /** Берём первую запись или создаём дефолтную */
    public CalculationConfig getConfig() {
        return repo.findAll().stream().findFirst()
                .orElseGet(() -> repo.save(
                        new CalculationConfig("HISTORICAL", 0.95, 1)
                ));
    }

    /** Обновляем существующую запись полями из входного DTO */
    public CalculationConfig updateConfig(CalculationConfig cfg) {
        CalculationConfig existing = getConfig();
        existing.setMethod(cfg.getMethod());
        existing.setConfidenceLevel(cfg.getConfidenceLevel());
        existing.setHorizonDays(cfg.getHorizonDays());
        return repo.save(existing);
    }
}
