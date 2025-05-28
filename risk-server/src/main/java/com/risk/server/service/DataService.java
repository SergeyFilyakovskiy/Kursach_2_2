package com.risk.server.service;

import com.risk.server.dto.DatasetDto;
import com.risk.server.dto.ValidationErrorDto;
import com.risk.server.model.DailyReturn;
import com.risk.server.model.Dataset;
import com.risk.server.repo.DailyReturnRepo;
import com.risk.server.repo.DatasetRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class DataService {
    private final DatasetRepository dsRepo;
    private final DailyReturnRepo    drRepo;
    private final ValidationService  valSvc;

    public DataService(DatasetRepository dsRepo,
                       DailyReturnRepo drRepo,
                       ValidationService valSvc) {
        this.dsRepo = dsRepo;
        this.drRepo = drRepo;
        this.valSvc = valSvc;
    }

    /**
     * Загружает CSV, сохраняет Dataset + доходности и возвращает DTO,
     * причём uploadedAt конвертируем в String.
     */
    public DatasetDto createDataset(MultipartFile file) throws Exception {
        // 1. сохраняем метаданные
        Dataset ds = new Dataset();
        ds.setName(file.getOriginalFilename());
        ds.setUploadedAt(LocalDateTime.now());
        ds = dsRepo.save(ds);

        // 2. парсим CSV → List<DailyReturn>
        List<DailyReturn> list = parseCsvToReturns(file, ds.getId());

        // 3. сохраняем доходности (с ценами)
        drRepo.saveAll(list);

        // 4. возвращаем DTO, uploadedAt как String
        return new DatasetDto(
                ds.getId(),
                ds.getName(),
                ds.getUploadedAt().toString()    // ← здесь
        );
    }

    /**
     * Список всех датасетов, отсортированных по uploadedAt DESC.
     * В DTO uploadedAt тоже в виде String.
     */
    public List<DatasetDto> listDatasets() {
        return dsRepo.findAll(Sort.by(Sort.Direction.DESC, "uploadedAt"))
                .stream()
                .map(d -> new DatasetDto(
                        d.getId(),
                        d.getName(),
                        d.getUploadedAt().toString()   // ← и здесь
                ))
                .toList();
    }

    /** Все записи доходностей для данного датасета */
    public List<DailyReturn> getData(Long datasetId) {
        return drRepo.findByDatasetId(datasetId, Sort.by("date"));
    }

    /** Валидация конкретного датасета */
    public List<ValidationErrorDto> validateDataset(Long datasetId) {
        return valSvc.validateDataset(datasetId);
    }

    /**
     * Простой CSV-парсер (первая строка — заголовок):
     * symbol,date,price,ret
     */
    private List<DailyReturn> parseCsvToReturns(MultipartFile file, Long dsId) throws Exception {
        List<DailyReturn> out = new ArrayList<>();
        BigDecimal prevPrice = null;

        try (var reader = new BufferedReader(new InputStreamReader(file.getInputStream()))) {
            // пропускаем заголовок
            String line = reader.readLine();
            while ((line = reader.readLine()) != null) {
                var parts = line.split(",", -1);
                String symbol = parts[0].trim();
                LocalDate date = LocalDate.parse(parts[1].trim());
                BigDecimal price = new BigDecimal(parts[2].trim());

                BigDecimal ret = BigDecimal.ZERO;
                if (prevPrice != null) {
                    ret = price
                            .subtract(prevPrice)
                            .divide(prevPrice, BigDecimal.ROUND_HALF_UP);
                }

                DailyReturn dr = new DailyReturn();
                dr.setDatasetId(dsId);
                dr.setSymbol(symbol);
                dr.setDate(date);
                dr.setPrice(price);
                dr.setRet(ret);

                out.add(dr);
                prevPrice = price;
            }
        }
        return out;
    }
}
