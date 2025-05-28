package com.risk.server.api;

import com.risk.server.dto.FinancialStatementDTO;
import com.risk.server.model.FinancialStatement;
import com.risk.server.repo.FinancialStatementRepository;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/fs")
public class FinancialStatementController {

    private final FinancialStatementRepository repo;

    public FinancialStatementController(FinancialStatementRepository r) { this.repo = r; }
    @PreAuthorize("hasAnyRole('ANALYST','ADMIN')")
    /** POST /api/fs  — создать запись */
    @PostMapping
    public ResponseEntity<FinancialStatement> create(@RequestBody FinancialStatementDTO dto) {
        FinancialStatement fs = new FinancialStatement();
        fs.setPeriod(dto.period());
        fs.setCurrency(dto.currency());
        fs.setSource(dto.source());
        return ResponseEntity.ok(repo.save(fs));
    }
    @PreAuthorize("hasAnyRole('ANALYST','ADMIN')")
    /** GET /api/fs/{id} — получить запись */
    @GetMapping("/{id}")
    public ResponseEntity<FinancialStatement> get(@PathVariable Long id) {
        return repo.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    @PreAuthorize("hasAnyRole('ANALYST','ADMIN')")
    @GetMapping
    public List<FinancialStatement> list() {
        return repo.findAll(Sort.by(Sort.Direction.DESC, "period"));
    }
}
