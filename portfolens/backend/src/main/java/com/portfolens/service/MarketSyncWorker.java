package com.portfolens.service;

import com.portfolens.dto.MarketDtos.*;
import com.portfolens.entity.HistoricalPrice;
import com.portfolens.exception.ApiException;
import com.portfolens.repository.HistoricalPriceRepository;
import com.portfolens.repository.StockRepository;
import java.time.Instant;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MarketSyncWorker {
    private final MarketDataClient client; private final HistoricalPriceRepository prices; private final StockRepository stocks;
    public MarketSyncWorker(MarketDataClient client,HistoricalPriceRepository prices,StockRepository stocks) { this.client=client; this.prices=prices; this.stocks=stocks; }
    @Transactional(propagation=Propagation.REQUIRES_NEW)
    public SyncResult sync(String symbol) {
        var s=stocks.findBySymbolIgnoreCase(symbol).orElseThrow(()->new ApiException(HttpStatus.NOT_FOUND,"STOCK_NOT_FOUND","Stock not found"));
        var response=client.history(s.getSymbol());
        if(response==null||response.records()==null||!s.getSymbol().equalsIgnoreCase(response.symbol())) throw new ApiException(HttpStatus.BAD_GATEWAY,"MARKET_DATA_INVALID","Market data service returned an invalid response");
        Instant fetched=response.last_updated()==null?Instant.now():response.last_updated(); int count=0;
        for(var r:response.records()) { if(r.date()==null||r.open()==null||r.high()==null||r.low()==null||r.close()==null||r.adjustedClose()==null||r.volume()<0) continue; var existing=prices.findByStockIdAndTradingDate(s.getId(),r.date()); if(existing.isPresent()) existing.get().update(r.open(),r.high(),r.low(),r.close(),r.adjustedClose(),r.volume(),response.source()==null?"Yahoo Finance":response.source(),fetched); else prices.save(new HistoricalPrice(s,r.date(),r.open(),r.high(),r.low(),r.close(),r.adjustedClose(),r.volume(),response.source()==null?"Yahoo Finance":response.source(),fetched)); count++; }
        return new SyncResult(s.getSymbol(),count,response.data_start_date(),response.data_end_date(),fetched);
    }
}
