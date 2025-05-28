package com.risk.server.repo;

import com.risk.server.model.DailyReturn;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

public interface DailyReturnRepo extends JpaRepository<DailyReturn, Long> {

    @Query("select d.ret from DailyReturn d where d.symbol = :sym order by d.date desc")
    List<BigDecimal> findLatestReturns(@Param("sym") String sym, org.springframework.data.domain.Pageable page);

    // Spring Data: по договорённости найдёт все записи с нужным datasetId
    List<DailyReturn> findByDatasetId(Long datasetId, Sort sort);
}
