package com.risk.server.service;

import com.risk.server.repo.DailyReturnRepo;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collections;
import java.util.List;

@Service
public class VaRService {

    private final DailyReturnRepo repo;

    public VaRService(DailyReturnRepo repo) {
        this.repo = repo;
    }

    /**
     * Расчёт Historical VaR по конкретному символу – у вас он уже был.
     */
    public BigDecimal calcHistoricalVaR(
            String symbol, int qty, BigDecimal price, int lookback
    ) {
        List<BigDecimal> rets = repo.findLatestReturns(
                symbol, PageRequest.of(0, lookback)
        );
        if (rets.size() < 50) {
            throw new IllegalStateException("Недостаточно данных history<50");
        }
        Collections.sort(rets);
        int idx = (int) Math.floor(0.05 * rets.size());
        BigDecimal worst = rets.get(idx);              // отрицательное число
        BigDecimal position = price.multiply(BigDecimal.valueOf(qty));
        return worst
                .abs()
                .multiply(position)
                .setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * *** НОВЫЙ метод: рассчитать Historical VaR по всему датасету. ***
     * Возвращаем просто число в виде BigDecimal (например, 0.05 для 5% VaR).
     */
    public BigDecimal calcHistoricalVaRByDataset(
            Long datasetId,
            double confidenceLevel,
            int lookbackDays
    ) {
        // Получаем из DailyReturnRepo последние lookbackDays процентов для datasetId:
        List<BigDecimal> rets = repo.findLatestReturnsByDataset(
                datasetId, PageRequest.of(0, lookbackDays)
        );

        if (rets.size() < lookbackDays) {
            throw new IllegalStateException(
                    "Недостаточно данных для VaR: нужно "
                            + lookbackDays + ", есть " + rets.size()
            );
        }
        Collections.sort(rets);
        // Определяем индекс: (1 - confidenceLevel) * N
        int idx = (int) Math.floor((1 - confidenceLevel) * rets.size());
        BigDecimal worst = rets.get(idx).abs();

        // Возвращаем в виде дроби (например, 0.05 для 5% VaR)
        return worst.setScale(6, RoundingMode.HALF_UP);
    }
}
