package com.risk.server.api;

import com.risk.server.dto.DatasetDto;
import com.risk.server.dto.HistoricalDataDto;
import com.risk.server.dto.ValidationErrorDto;
import com.risk.server.model.DailyReturn;
import com.risk.server.service.DataService;
import com.risk.server.service.ValidationService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

@RestController
@RequestMapping("/api/data")
public class DataController {

    private final DataService dataSvc;
    private final ValidationService valSvc;

    public DataController(DataService dataSvc,
                          ValidationService valSvc) {
        this.dataSvc = dataSvc;
        this.valSvc  = valSvc;
    }

    /** 1) загрузка CSV → новый Dataset + DailyReturn */
    @PostMapping("/upload")
    public DatasetDto upload(@RequestParam("file") MultipartFile file) throws Exception {
        return dataSvc.createDataset(file);
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
    public List<HistoricalDataDto> listData(@PathVariable Long datasetId) {
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
    public List<ValidationErrorDto> validate(@PathVariable Long datasetId) {
        return valSvc.validateDataset(datasetId);
    }

}
