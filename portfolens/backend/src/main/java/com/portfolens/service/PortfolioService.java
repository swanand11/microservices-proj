package com.portfolens.service;

import com.portfolens.dto.*;
import com.portfolens.entity.*;
import com.portfolens.exception.ApiException;
import com.portfolens.repository.*;
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
    private final PortfolioRepository portfolios; private final UserRepository users; private final StockRepository stocks; private final HoldingRepository holdings; private final PortfolioTransactionRepository transactions;
    public PortfolioService(PortfolioRepository portfolios, UserRepository users, StockRepository stocks, HoldingRepository holdings, PortfolioTransactionRepository transactions) { this.portfolios = portfolios; this.users = users; this.stocks = stocks; this.holdings = holdings; this.transactions = transactions; }
    private UUID userId(String id) { try { return UUID.fromString(id); } catch (IllegalArgumentException ex) { throw new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Authentication is required"); } }
    private Portfolio owned(UUID id, String owner) { return portfolios.findByIdAndUserId(id, userId(owner)).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "PORTFOLIO_NOT_FOUND", "Portfolio not found")); }
    private Stock stock(UUID id) { return stocks.findByIdAndActiveTrue(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "STOCK_NOT_FOUND", "Stock not found")); }
    @Transactional(readOnly=true) public List<PortfolioResponse> list(String owner) { return portfolios.findAllByUserIdOrderByCreatedAtDesc(userId(owner)).stream().map(PortfolioResponse::from).toList(); }
    @Transactional public PortfolioResponse create(String owner, PortfolioRequest req) { var user=users.findById(userId(owner)).orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED,"UNAUTHORIZED","Authenticated user not found")); return PortfolioResponse.from(portfolios.save(new Portfolio(user, req.name().trim(), req.description()))); }
    @Transactional(readOnly=true) public PortfolioResponse get(UUID id,String owner) { return PortfolioResponse.from(owned(id,owner)); }
    @Transactional public PortfolioResponse update(UUID id,String owner,PortfolioRequest req) { var p=owned(id,owner); p.update(req.name().trim(),req.description()); return PortfolioResponse.from(p); }
    @Transactional public void delete(UUID id,String owner) { owned(id,owner); transactions.deleteAllByPortfolioId(id); holdings.deleteAllByPortfolioId(id); portfolios.deleteById(id); }
    @Transactional(readOnly=true) public List<StockResponse> searchStocks(String q,String sector) { return stocks.search(blankToNull(q),blankToNull(sector)).stream().map(StockResponse::from).toList(); }
    @Transactional(readOnly=true) public StockResponse getStock(UUID id) { return StockResponse.from(stock(id)); }
    @Transactional(readOnly=true) public List<HoldingResponse> listHoldings(UUID id,String owner) { owned(id,owner); return holdings.findAllByPortfolioIdOrderByStockSymbol(id).stream().map(HoldingResponse::from).toList(); }
    @Transactional public HoldingResponse addHolding(UUID id,String owner,HoldingRequest req) { var p=owned(id,owner); var s=stock(req.stockId()); if(holdings.existsByPortfolioIdAndStockId(id,s.getId())) throw new ApiException(HttpStatus.CONFLICT,"DUPLICATE_HOLDING","This portfolio already has a holding for that stock"); return HoldingResponse.from(holdings.save(new Holding(p,s,req.quantity(),req.averageBuyPrice()))); }
    @Transactional public HoldingResponse updateHolding(UUID id,UUID hid,String owner,HoldingRequest req) { var p=owned(id,owner); var h=holdings.findByIdAndPortfolioId(hid,p.getId()).orElseThrow(()->new ApiException(HttpStatus.NOT_FOUND,"HOLDING_NOT_FOUND","Holding not found")); if(!h.getStock().getId().equals(req.stockId())) throw new ApiException(HttpStatus.BAD_REQUEST,"STOCK_IMMUTABLE","A holding's stock cannot be changed"); h.update(req.quantity(),req.averageBuyPrice()); return HoldingResponse.from(h); }
    @Transactional public void deleteHolding(UUID id,UUID hid,String owner) { owned(id,owner); var h=holdings.findByIdAndPortfolioId(hid,id).orElseThrow(()->new ApiException(HttpStatus.NOT_FOUND,"HOLDING_NOT_FOUND","Holding not found")); holdings.delete(h); }
    @Transactional(readOnly=true) public List<TransactionResponse> listTransactions(UUID id,String owner) { owned(id,owner); return transactions.findAllByPortfolioIdOrderByTransactionDateDescCreatedAtDesc(id).stream().map(TransactionResponse::from).toList(); }
    @Transactional public TransactionResponse recordTransaction(UUID id,String owner,TransactionRequest req) {
        var p=owned(id,owner); var s=stock(req.stockId()); var existing=holdings.findByPortfolioIdAndStockId(id,s.getId()).orElse(null);
        if(req.transactionType()==PortfolioTransaction.Type.BUY) {
            if(existing==null) existing=new Holding(p,s,req.quantity(),req.price());
            else { var total=existing.getQuantity().add(req.quantity()); var weighted=existing.getQuantity().multiply(existing.getAverageBuyPrice()).add(req.quantity().multiply(req.price())).divide(total,8,RoundingMode.HALF_UP); existing.update(total,weighted); }
            holdings.save(existing);
        } else {
            if(existing==null || existing.getQuantity().compareTo(req.quantity())<0) throw new ApiException(HttpStatus.BAD_REQUEST,"INSUFFICIENT_HOLDINGS","Cannot sell more shares than are currently held");
            var remainder=existing.getQuantity().subtract(req.quantity()); if(remainder.signum()==0) holdings.delete(existing); else existing.update(remainder,existing.getAverageBuyPrice());
        }
        var t=new PortfolioTransaction(p,s,req.transactionType(),req.quantity(),req.price(),req.transactionDate()==null?Instant.now():req.transactionDate());
        return TransactionResponse.from(transactions.save(t));
    }
    private String blankToNull(String s) { return s==null||s.isBlank()?null:s.trim(); }
}
