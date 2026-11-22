package com.portfolens.controller;
import com.portfolens.dto.*;
import com.portfolens.service.PortfolioService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api")
public class PortfolioController {
    private final PortfolioService service;
    public PortfolioController(PortfolioService service) { this.service=service; }
    @GetMapping("/portfolios") public List<PortfolioResponse> list(Authentication a) { return service.list(a.getName()); }
    @PostMapping("/portfolios") @ResponseStatus(HttpStatus.CREATED) public PortfolioResponse create(Authentication a,@Valid @RequestBody PortfolioRequest r) { return service.create(a.getName(),r); }
    @GetMapping("/portfolios/{id}") public PortfolioResponse get(@PathVariable UUID id,Authentication a) { return service.get(id,a.getName()); }
    @PutMapping("/portfolios/{id}") public PortfolioResponse update(@PathVariable UUID id,Authentication a,@Valid @RequestBody PortfolioRequest r) { return service.update(id,a.getName(),r); }
    @DeleteMapping("/portfolios/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) public void delete(@PathVariable UUID id,Authentication a) { service.delete(id,a.getName()); }
    @GetMapping("/stocks") public List<StockResponse> stocks(@RequestParam(required=false) String q,@RequestParam(required=false) String sector) { return service.searchStocks(q,sector); }
    @GetMapping("/stocks/{id}") public StockResponse stock(@PathVariable UUID id) { return service.getStock(id); }
    @GetMapping("/portfolios/{id}/holdings") public List<HoldingResponse> holdings(@PathVariable UUID id,Authentication a) { return service.listHoldings(id,a.getName()); }
    @GetMapping("/portfolios/{id}/transactions") public List<TransactionResponse> transactions(@PathVariable UUID id,Authentication a) { return service.listTransactions(id,a.getName()); }
    @PostMapping("/portfolios/{id}/transactions") @ResponseStatus(HttpStatus.CREATED) public TransactionResponse record(@PathVariable UUID id,Authentication a,@RequestHeader("Idempotency-Key") String idempotencyKey,@Valid @RequestBody TransactionRequest r) { return service.recordTransaction(id,a.getName(),r,idempotencyKey); }
}
