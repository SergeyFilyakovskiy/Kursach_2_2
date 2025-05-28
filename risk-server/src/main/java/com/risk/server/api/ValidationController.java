package com.risk.server.api;

import com.risk.server.dto.ValidationErrorDto;
import com.risk.server.model.ValidationError;
import com.risk.server.service.ValidationService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/validation")
public class ValidationController {

    private final ValidationService vs;

    public ValidationController(ValidationService vs) {
        this.vs = vs;
    }

    /** Валидация всего (всех датасетов) */
    @GetMapping(
            path = "/all",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public List<ValidationErrorDto> allErrors() {
        return vs.validateAll();
    }

    /** Валидация конкретного датасета по ID */
    @GetMapping(
            path = "/{datasetId}",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public List<ValidationErrorDto> errors(
            @PathVariable Long datasetId
    ) {
        return vs.validateDataset(datasetId);
    }
}
