package com.portfolens.controller;
import com.portfolens.dto.MarketDtos.*;
import com.portfolens.service.MarketDataService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/market")
public class MarketDataController {
    private final MarketDataService service;
    public MarketDataController(MarketDataService service) { this.service=service; }
    @GetMapping("/stocks") public List<com.portfolens.dto.StockResponse> stocks() { return service.stocksList(); }
    @GetMapping("/stocks/{symbol}/history") public List<PriceResponse> history(@PathVariable String symbol,@RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate start,@RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate end) { return service.history(symbol,start,end); }
    @GetMapping("/stocks/{symbol}/latest") public PriceResponse latest(@PathVariable String symbol) { return service.latest(symbol); }
    @PostMapping("/sync/{symbol}") @ResponseStatus(HttpStatus.OK) public SyncResult sync(@PathVariable String symbol,@RequestHeader(value="X-Market-Sync-Key",required=false) String key) { return service.sync(symbol,key); }
    @PostMapping("/sync/bulk") public List<SyncResult> bulk(@RequestHeader(value="X-Market-Sync-Key",required=false) String key,@Valid @RequestBody(required=false) SyncRequest req) { return service.syncBulk(key); }
    @GetMapping("/sync/status") public String syncStatus() { return service.pythonStatus(); }
    @GetMapping("/sync/status/{symbol}") public SyncStatus status(@PathVariable String symbol) { return service.status(symbol); }
}
