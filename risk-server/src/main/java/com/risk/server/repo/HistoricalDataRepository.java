package com.risk.server.repo;

import com.risk.server.model.HistoricalData;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HistoricalDataRepository extends JpaRepository<HistoricalData, Long> {}