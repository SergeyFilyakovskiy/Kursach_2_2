package com.risk.server.api;

import com.risk.server.dto.CalculationConfigDto;
import com.risk.server.dto.DatasetDto;
import com.risk.server.dto.HistoricalDataDto;
import com.risk.server.dto.ValidationErrorDto;
import com.risk.server.model.DailyReturn;
import com.risk.server.service.CalculationService;
import com.risk.server.service.DataService;
import com.risk.server.service.VaRService;
import com.risk.server.service.ValidationService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/data")
public class DataController {

    private final DataService dataSvc;
    private final ValidationService valSvc;
    private final VaRService varSvc;
    private final CalculationService calcSvc;

    public DataController(DataService dataSvc,
                          ValidationService valSvc,
                          VaRService varSvc,
                          CalculationService calcSvc) {
        this.dataSvc = dataSvc;
        this.valSvc  = valSvc;
        this.varSvc  = varSvc;
        this.calcSvc  = calcSvc;
    }
    @PostMapping(path = "/echo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public String echo(@RequestParam("file") MultipartFile file) {
        return "Got " + file.getOriginalFilename() + " (" + file.getSize() + " bytes)";
    }
    /** 1) загрузка CSV → новый Dataset + DailyReturn */
    @PostMapping(
            path = "/upload",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public DatasetDto upload(@RequestParam("file") MultipartFile file) throws Exception {
        System.out.println(">>> upload() called: name="
                + file.getOriginalFilename()
                + ", size=" + file.getSize());
        return dataSvc.createDataset(file);
    }
    @GetMapping("/ping")
    public String ping() {
        return "pong";
    }
    /** 2) список загруженных CSV */
    @GetMapping("/datasets")
    public List<DatasetDto> listDatasets() {
        return dataSvc.listDatasets();
    }
    /** 3) доходности (с ценами!) для одного Dataset */
    @GetMapping(
            path     = "/{datasetId}/history",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public List<HistoricalDataDto> listData(@PathVariable("datasetId") Long datasetId) {
        return dataSvc.getData(datasetId).stream()
                .map(dr -> new HistoricalDataDto(
                        dr.getId(),               // <— вот этот id
                        dr.getSymbol(),
                        dr.getDate().toString(),
                        dr.getPrice(),
                        dr.getRet()
                ))
                .toList();
    }
    private List<HistoricalDataDto> mapToDto(List<DailyReturn> list) {
        return list.stream()
                .map(dr -> new HistoricalDataDto(
                        dr.getId(),
                        dr.getSymbol(),
                        dr.getDate().toString(),
                        dr.getPrice(),
                        dr.getRet()))
                .toList();
    }
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public List<HistoricalDataDto> listDataByParam(@RequestParam("datasetId") Long datasetId) {
        System.out.println(">>> listDataByParam() called with datasetId=" + datasetId);
        return mapToDto(dataSvc.getData(datasetId));
    }

    /** 4) Валидация конкретного датасета */
    @GetMapping(
            path     = "/{datasetId}/validate",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public List<ValidationErrorDto> validate(@PathVariable("datasetId") Long datasetId) {
        return valSvc.validateDataset(datasetId);
    }
    @GetMapping(
            path = "/config",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public CalculationConfigDto getConfig() {
        return calcSvc.getConfig();
    }

    /**
     * 6) Сохранить (или обновить) настройки расчёта VaR
     */
    @PutMapping(
            path = "/config",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public CalculationConfigDto updateConfig(@RequestBody CalculationConfigDto cfg) {
        return calcSvc.updateConfig(cfg);
    }

    /**
     * 7) Рассчитать VaR для всего датасета на основе текущих настроек
     *    Ендпоинт может вернуть, например, просто число (BigDecimal), или обёртку.
     */
    @GetMapping(
            path = "/{datasetId}/var",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public BigDecimal calculateVaR(
            @PathVariable("datasetId") Long datasetId
    ) {
        // Сначала получаем из БД ваш конфиг (confidenceLevel + horizonDays)
        CalculationConfigDto cfg = calcSvc.getConfig();
        // Дальше вызываем VaRService, который вернёт цифру (например, 0.05 ... процент потерь)
        return varSvc.calcHistoricalVaRByDataset(
                datasetId,
                cfg.confidenceLevel(),
                cfg.horizonDays()
        );
    }
}
