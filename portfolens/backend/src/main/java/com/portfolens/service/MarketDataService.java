package com.portfolens.service;

import com.portfolens.dto.MarketDtos.*;
import com.portfolens.entity.HistoricalPrice;
import com.portfolens.exception.ApiException;
import com.portfolens.repository.HistoricalPriceRepository;
import com.portfolens.repository.StockRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MarketDataService {
    private final MarketDataClient client; private final HistoricalPriceRepository prices; private final StockRepository stocks; private final String syncKey; private final MarketSyncWorker worker;
    public MarketDataService(MarketDataClient client, HistoricalPriceRepository prices, StockRepository stocks, MarketSyncWorker worker, @Value("${app.market-data.sync-key:}") String syncKey) { this.client=client; this.prices=prices; this.stocks=stocks; this.worker=worker; this.syncKey=syncKey; }
    private com.portfolens.entity.Stock stock(String symbol) { return stocks.findBySymbolIgnoreCase(symbol).orElseThrow(()->new ApiException(HttpStatus.NOT_FOUND,"STOCK_NOT_FOUND","Stock not found")); }
    @Transactional(readOnly=true) public List<PriceResponse> history(String symbol,LocalDate start,LocalDate end) {
        var s=stock(symbol); LocalDate min=start==null?LocalDate.of(1900,1,1):start; LocalDate max=end==null?LocalDate.now():end;
        if(min.isAfter(max)) throw new ApiException(HttpStatus.BAD_REQUEST,"INVALID_DATE_RANGE","start must be on or before end");
        return prices.findAllByStockSymbolAndTradingDateBetweenOrderByTradingDateAsc(s.getSymbol(),min,max).stream().map(this::response).toList();
    }
    @Transactional(readOnly=true) public PriceResponse latest(String symbol) { return prices.findFirstByStockSymbolOrderByTradingDateDesc(stock(symbol).getSymbol()).map(this::response).orElseThrow(()->new ApiException(HttpStatus.NOT_FOUND,"PRICE_NOT_FOUND","No historical prices are available")); }
    public SyncResult sync(String symbol,String suppliedKey) { requireKey(suppliedKey); return worker.sync(symbol); }
    public List<SyncResult> syncBulk(String suppliedKey) {
        requireKey(suppliedKey);
        return stocks.findAll().stream().filter(s->s.isActive()).map(s->{
            try { return worker.sync(s.getSymbol()); }
            catch (RuntimeException ex) { return new SyncResult(s.getSymbol(),0,null,null,null); }
        }).toList();
    }
    @Transactional(readOnly=true) public SyncStatus status(String symbol) { var s=stock(symbol); var latest=prices.findFirstByStockSymbolOrderByTradingDateDesc(s.getSymbol()).orElse(null); return new SyncStatus(s.getSymbol(),prices.countByStockSymbol(s.getSymbol()),latest==null?null:latest.getTradingDate(),latest==null?null:latest.getFetchedAt()); }
    public List<com.portfolens.dto.StockResponse> stocksList() { return stocks.findAll().stream().filter(s->s.isActive()).map(s->new com.portfolens.dto.StockResponse(s.getId(),s.getSymbol(),s.getCompanyName(),s.getExchange(),s.getBseScripCode(),s.getSector(),s.getIndustry(),s.isActive(),s.getCreatedAt())).toList(); }
    public String pythonStatus() { return client.status(); }
    private void requireKey(String supplied) { if(syncKey==null||syncKey.isBlank()||supplied==null||!java.security.MessageDigest.isEqual(syncKey.getBytes(java.nio.charset.StandardCharsets.UTF_8),supplied.getBytes(java.nio.charset.StandardCharsets.UTF_8))) throw new ApiException(HttpStatus.FORBIDDEN,"SYNC_FORBIDDEN","Market data synchronization is not enabled or the operator key is invalid"); }
    private PriceResponse response(HistoricalPrice p) { return new PriceResponse(p.getStock().getSymbol(),p.getStock().getCompanyName(),p.getTradingDate(),p.getOpen(),p.getHigh(),p.getLow(),p.getClose(),p.getAdjustedClose(),p.getVolume(),p.getSource(),p.getFetchedAt()); }
}
