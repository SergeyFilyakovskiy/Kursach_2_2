package com.risk.server.api;

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
    private final CalculationService cfgSvc;

    public DataController(DataService dataSvc,
                          ValidationService valSvc,
                          VaRService varSvc,
                          CalculationService cfgSvc) {
        this.dataSvc = dataSvc;
        this.valSvc  = valSvc;
        this.varSvc  = varSvc;
        this.cfgSvc  = cfgSvc;
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
            path     = "/{datasetId}",
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
            path     = "/{datasetId}/var",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public BigDecimal varByDataset(
            @PathVariable Long datasetId
    ) {
        // а) читаем текущие настройки пользователя
        var cfg = cfgSvc.getConfig();
        // б) делегируем в сервис VaR
        return varSvc.calcHistoricalVaRByDataset(
                datasetId,
                cfg.confidenceLevel(),
                cfg.horizonDays()
        );
    }

}
