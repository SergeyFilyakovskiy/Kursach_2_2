package com.risk.server.service;
import com.risk.server.repo.DailyReturnRepo;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import java.math.*;
import java.util.Collections;
import java.util.List;

@Service
public class VaRService {
    private final DailyReturnRepo repo;
    public VaRService(DailyReturnRepo r){ this.repo=r; }

    public BigDecimal calcHistoricalVaR(
            String symbol, int qty, BigDecimal price, int lookback){
        List<BigDecimal> rets = repo.findLatestReturns(
                symbol, PageRequest.of(0, lookback));
        if(rets.size()<50) throw new IllegalStateException("history<50");
        Collections.sort(rets);
        int idx = (int)Math.floor(0.05*rets.size());
        BigDecimal worst = rets.get(idx);              // отрицательное
        BigDecimal position = price.multiply(BigDecimal.valueOf(qty));
        return worst.abs().multiply(position).setScale(2, RoundingMode.HALF_UP);
    }
}
