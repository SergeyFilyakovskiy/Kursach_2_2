package com.risk.server.service;

import com.risk.server.dto.CalculationConfigDto;
import org.springframework.stereotype.Service;

@Service
public class CalculationService {

    // изначальные дефолтные настройки
    private CalculationConfigDto config =
            new CalculationConfigDto("HISTORICAL", 0.95, 1);

    /**
     * Вернуть текущие настройки VaR.
     */
    public CalculationConfigDto getConfig() {
        return config;
    }

    /**
     * Обновить настройки VaR.
     * @return true = успешно
     */
    public boolean updateConfig(CalculationConfigDto dto) {
        this.config = dto;
        return true;
    }
}
