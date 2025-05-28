package com.risk.server.api;

import com.risk.server.model.CalculationConfig;
import com.risk.server.service.ConfigService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/config")
public class ConfigController {
    private final ConfigService service;

    public ConfigController(ConfigService service) {
        this.service = service;
    }

    /** GET /api/config — вернуть текущую конфигурацию */
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public CalculationConfig getConfig() {
        return service.getConfig();
    }

    /** POST /api/config — обновить конфигурацию */
    @PostMapping(
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<CalculationConfig> updateConfig(
            @RequestBody CalculationConfig cfg) {
        return ResponseEntity.ok(service.updateConfig(cfg));
    }
}
