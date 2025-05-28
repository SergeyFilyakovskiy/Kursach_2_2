package com.risk.server.api;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.risk.server.model.RiskPosition;
import com.risk.server.repo.RiskPositionRepo;
import com.risk.server.service.VaRService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController @RequestMapping("/api/var")
public class VaRController {

    private final VaRService varSvc;
    private final RiskPositionRepo posRepo;
    public VaRController(VaRService v, RiskPositionRepo p){ this.varSvc=v; this.posRepo=p; }

    /* DTO для создания позиции */
    public record PosDTO(
            String symbol,
            int quantity,
            BigDecimal lastPrice,
            @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
            LocalDate valDate) {}

    @PostMapping("/position")
    public RiskPosition add(@RequestBody PosDTO dto){
        RiskPosition p = new RiskPosition();
        p.setSymbol(dto.symbol());
        p.setQuantity(dto.quantity());
        p.setLastPrice(dto.lastPrice());
        p.setValDate(dto.valDate());
        return posRepo.save(p);
    }

    @GetMapping("/{id}/api/var")
    public ResponseEntity<?> var(@PathVariable Long id,
                                 @RequestParam(defaultValue="250") int lookback) {
        return posRepo.findById(id)
                .map(p -> {
                    try {
                        BigDecimal var = varSvc.calcHistoricalVaR(
                                p.getSymbol(), p.getQuantity(), p.getLastPrice(), lookback);
                        return ResponseEntity.ok(var);
                    } catch (IllegalStateException ex) {
                        return ResponseEntity
                                .badRequest()
                                .body(Map.of("error", ex.getMessage()));
                    }
                })
                .orElse(ResponseEntity.status(404).body(Map.of("error","Position not found")));
    }
    @PreAuthorize("isAuthenticated()")
    @GetMapping
    public List<RiskPosition> listAll() {
        return posRepo.findAll();
    }

}
