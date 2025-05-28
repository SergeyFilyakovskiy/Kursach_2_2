// risk-server/src/main/java/com/risk/server/repo/ValidationErrorRepository.java
package com.risk.server.repo;

import com.risk.server.model.ValidationError;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ValidationErrorRepository extends JpaRepository<ValidationError, Long> {

}