package com.risk.server.service;

import com.risk.server.model.HistoricalData;
import com.risk.server.repo.HistoricalDataRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class DataIngestionService {
    private final HistoricalDataRepository repo;

    public DataIngestionService(HistoricalDataRepository repo) {
        this.repo = repo;
    }

    public List<HistoricalData> ingestCsv(MultipartFile file) throws Exception {
        List<HistoricalData> saved = new ArrayList<>();
        try (var reader = new BufferedReader(new InputStreamReader(file.getInputStream()))) {
            String line;
            // предположим, первая строка — заголовок
            reader.readLine();
            while ((line = reader.readLine()) != null) {
                String[] cols = line.split(",");
                HistoricalData d = new HistoricalData();
                d.setSymbol(cols[0].trim());
                d.setDate(LocalDate.parse(cols[1].trim()));
                d.setPrice(new BigDecimal(cols[2].trim()));
                // если в CSV есть доходность:
                if (cols.length > 3 && !cols[3].isBlank()) {
                    d.setRet(new BigDecimal(cols[3].trim()));
                }
                saved.add(repo.save(d));
            }
        }
        return saved;
    }
}