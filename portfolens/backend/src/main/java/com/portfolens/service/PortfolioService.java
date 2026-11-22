package com.portfolens.service;

import com.portfolens.dto.*;
import com.portfolens.entity.*;
import com.portfolens.exception.ApiException;
import com.portfolens.repository.*;
import com.portfolens.dto.PortfolioTransactionRecorded;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PortfolioService {
    private final PortfolioRepository portfolios; private final UserRepository users; private final StockRepository stocks; private final HoldingRepository holdings; private final PortfolioTransactionRepository transactions; private final OutboxEventRepository outbox; private final ObjectMapper objectMapper;
    public PortfolioService(PortfolioRepository portfolios, UserRepository users, StockRepository stocks, HoldingRepository holdings, PortfolioTransactionRepository transactions, OutboxEventRepository outbox, ObjectMapper objectMapper) { this.portfolios = portfolios; this.users = users; this.stocks = stocks; this.holdings = holdings; this.transactions = transactions; this.outbox = outbox; this.objectMapper = objectMapper; }
    private UUID userId(String id) { try { return UUID.fromString(id); } catch (IllegalArgumentException ex) { throw new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Authentication is required"); } }
    private Portfolio owned(UUID id, String owner) { return portfolios.findByIdAndUserId(id, userId(owner)).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "PORTFOLIO_NOT_FOUND", "Portfolio not found")); }
    private Portfolio ownedForUpdate(UUID id, String owner) { return portfolios.lockByIdAndUserId(id, userId(owner)).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "PORTFOLIO_NOT_FOUND", "Portfolio not found")); }
    private Stock stock(UUID id) { return stocks.findByIdAndActiveTrue(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "STOCK_NOT_FOUND", "Stock not found")); }
    @Transactional(readOnly=true) public List<PortfolioResponse> list(String owner) { return portfolios.findAllByUserIdOrderByCreatedAtDesc(userId(owner)).stream().map(PortfolioResponse::from).toList(); }
    @Transactional public PortfolioResponse create(String owner, PortfolioRequest req) { var user=users.findById(userId(owner)).orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED,"UNAUTHORIZED","Authenticated user not found")); return PortfolioResponse.from(portfolios.save(new Portfolio(user, req.name().trim(), req.description()))); }
    @Transactional(readOnly=true) public PortfolioResponse get(UUID id,String owner) { return PortfolioResponse.from(owned(id,owner)); }
    @Transactional public PortfolioResponse update(UUID id,String owner,PortfolioRequest req) { var p=owned(id,owner); p.update(req.name().trim(),req.description()); return PortfolioResponse.from(p); }
    @Transactional public void delete(UUID id,String owner) { owned(id,owner); transactions.deleteAllByPortfolioId(id); holdings.deleteAllByPortfolioId(id); portfolios.deleteById(id); }
    @Transactional(readOnly=true) public List<StockResponse> searchStocks(String q,String sector) { return stocks.search(blankToNull(q),blankToNull(sector)).stream().map(StockResponse::from).toList(); }
    @Transactional(readOnly=true) public StockResponse getStock(UUID id) { return StockResponse.from(stock(id)); }
    @Transactional(readOnly=true) public List<HoldingResponse> listHoldings(UUID id,String owner) { owned(id,owner); return holdings.findAllByPortfolioIdOrderByStockSymbol(id).stream().map(HoldingResponse::from).toList(); }
    @Transactional(readOnly=true) public List<TransactionResponse> listTransactions(UUID id,String owner) { owned(id,owner); return transactions.findAllByPortfolioIdOrderByTransactionDateDescCreatedAtDesc(id).stream().map(TransactionResponse::from).toList(); }
    @Transactional public TransactionResponse recordTransaction(UUID id,String owner,TransactionRequest req,String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank() || idempotencyKey.length() > 100) throw new ApiException(HttpStatus.BAD_REQUEST,"INVALID_IDEMPOTENCY_KEY","Idempotency-Key must contain 1 to 100 characters");
        var p=ownedForUpdate(id,owner);
        var prior=transactions.findByPortfolioIdAndIdempotencyKey(id,idempotencyKey).orElse(null);
        if (prior != null) {
            boolean same=prior.getStock().getId().equals(req.stockId()) && prior.getTransactionType()==req.transactionType() && prior.getQuantity().compareTo(req.quantity())==0 && prior.getPrice().compareTo(req.price())==0 && (req.transactionDate()==null || java.util.Objects.equals(prior.getTransactionDate(),req.transactionDate()));
            if(!same) throw new ApiException(HttpStatus.CONFLICT,"IDEMPOTENCY_KEY_REUSED","This Idempotency-Key was already used for a different transaction");
            return TransactionResponse.from(prior);
        }
        var s=stock(req.stockId()); var existing=holdings.findByPortfolioIdAndStockId(id,s.getId()).orElse(null);
        if(req.transactionType()==PortfolioTransaction.Type.BUY) {
            if(existing==null) existing=new Holding(p,s,req.quantity(),req.price());
            else { var total=existing.getQuantity().add(req.quantity()); var weighted=existing.getQuantity().multiply(existing.getAverageBuyPrice()).add(req.quantity().multiply(req.price())).divide(total,8,RoundingMode.HALF_UP); existing.update(total,weighted); }
            holdings.save(existing);
        } else {
            if(existing==null || existing.getQuantity().compareTo(req.quantity())<0) throw new ApiException(HttpStatus.BAD_REQUEST,"INSUFFICIENT_HOLDINGS","Cannot sell more shares than are currently held");
            var remainder=existing.getQuantity().subtract(req.quantity()); if(remainder.signum()==0) holdings.delete(existing); else existing.update(remainder,existing.getAverageBuyPrice());
        }
        var t=new PortfolioTransaction(p,s,req.transactionType(),req.quantity(),req.price(),req.transactionDate()==null?Instant.now():req.transactionDate(),idempotencyKey);
        t=transactions.saveAndFlush(t);
        var eventId=t.getId();
        try {
            var payload=objectMapper.writeValueAsString(PortfolioTransactionRecorded.from(t,eventId));
            outbox.save(new OutboxEvent(eventId,"PortfolioTransactionRecorded","portfolio.transaction.recorded",payload));
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Could not serialize transaction event",ex);
        }
        return TransactionResponse.from(t);
    }
    private String blankToNull(String s) { return s==null||s.isBlank()?null:s.trim(); }
}
