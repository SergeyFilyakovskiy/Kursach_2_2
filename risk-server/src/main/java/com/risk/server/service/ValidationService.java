// src/main/java/com/risk/server/service/ValidationService.java
package com.risk.server.service;

import com.risk.server.dto.ValidationErrorDto;
import com.risk.server.model.DailyReturn;
import com.risk.server.repo.DailyReturnRepo;
import com.risk.server.repo.DatasetRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class ValidationService {
    private final DailyReturnRepo drRepo;
    private final DatasetRepository dsRepo;


    public ValidationService(DailyReturnRepo drRepo, DatasetRepository dsRepo) {
        this.drRepo = drRepo;
        this.dsRepo  = dsRepo;
    }

    public List<ValidationErrorDto> validateAll() {
        return dsRepo.findAll().stream()
                .flatMap(d -> validateDataset(d.getId()).stream())
                .collect(Collectors.toList());
    }
    /** Валидация по datasetId */
    public List<ValidationErrorDto> validateDataset(Long datasetId) {
        // Ваш алгоритм валидации: drRepo.findByDatasetId(datasetId, Sort.by("date"))
        // Преобразовать нарушенные строки в ValidationErrorDto
        return drRepo.findByDatasetId(datasetId, Sort.by("date"))
                .stream()
                .flatMap(dr -> {
                    // пример: если ret == 0 — считаем ошибкой
                    if (dr.getRet().compareTo(BigDecimal.ZERO) == 0) {
                        return Stream.of(new ValidationErrorDto(
                                dr.getId(),
                                dr.getSymbol(),
                                dr.getDate().toString(),
                                "ret",
                                "Нулевая доходность"
                        ));
                    }
                    return Stream.empty();
                })
                .toList();
    }
}
