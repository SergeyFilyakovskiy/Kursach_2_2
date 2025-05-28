package com.risk.server.repo;
import com.risk.server.model.RiskPosition;
import org.springframework.data.jpa.repository.JpaRepository;
public interface RiskPositionRepo extends JpaRepository<RiskPosition,Long>{}