package com.risk.server.repo;

import com.risk.server.model.DailyReturn;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

public interface DailyReturnRepo extends JpaRepository<DailyReturn,Long> {

    /**
     * 1) Список последних доходностей по символу
     */
    @Query("SELECT d.ret FROM DailyReturn d " +
            " WHERE d.symbol = :sym" +
            " ORDER BY d.date DESC")
    List<BigDecimal> findLatestReturns(
            @Param("sym") String symbol,
            Pageable page
    );

    /**
     * 2) Список последних доходностей по датасету
     */
    @Query("SELECT d.ret FROM DailyReturn d " +
            " WHERE d.datasetId = :dsId" +
            " ORDER BY d.date DESC")
    List<BigDecimal> findLatestReturnsByDataset(
            @Param("dsId") Long datasetId,
            Pageable page
    );

    /**
     * 3) Для других нужд у вас уже есть
     */
    List<DailyReturn> findByDatasetId(Long datasetId, Sort sort);
}
