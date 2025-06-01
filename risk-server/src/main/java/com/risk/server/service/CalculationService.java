package com.risk.server.service;

import com.risk.server.dto.CalculationConfigDto;
import com.risk.server.model.CalculationConfig;
import com.risk.server.repo.CalculationConfigRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CalculationService {

    private final CalculationConfigRepository cfgRepo;

    public CalculationService(CalculationConfigRepository cfgRepo) {
        this.cfgRepo = cfgRepo;
    }

    /**
     * Вернуть текущий конфиг VaR. Если ещё нет ни одной записи, создаём дефолтный.
     */
    @Transactional(readOnly = true)
    public CalculationConfigDto getConfig() {
        CalculationConfig cfg = cfgRepo.findFirstByOrderByIdAsc();
        if (cfg == null) {
            // если ещё нет строки в базе, добавим дефолтные значения:
            cfg = new CalculationConfig("HISTORICAL", 0.95, 1);
            cfg = cfgRepo.save(cfg);
        }
        return new CalculationConfigDto(
                cfg.getId(),
                cfg.getMethod(),
                cfg.getConfidenceLevel(),
                cfg.getHorizonDays()
        );
    }

    /**
     * Обновить (или создать) конфигурацию.
     */
    @Transactional
    public CalculationConfigDto updateConfig(CalculationConfigDto dto) {
        CalculationConfig cfg = cfgRepo.findFirstByOrderByIdAsc();
        if (cfg == null) {
            // Если не было ранее сохранённого — создаём новую запись
            cfg = new CalculationConfig();
        }
        cfg.setMethod(dto.method());
        cfg.setConfidenceLevel(dto.confidenceLevel());
        cfg.setHorizonDays(dto.horizonDays());
        cfg = cfgRepo.save(cfg);

        return new CalculationConfigDto(
                cfg.getId(),
                cfg.getMethod(),
                cfg.getConfidenceLevel(),
                cfg.getHorizonDays()
        );
    }
}
