package com.portfolens.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.portfolens.dto.TransactionRequest;
import com.portfolens.entity.Portfolio;
import com.portfolens.entity.PortfolioTransaction;
import com.portfolens.entity.Stock;
import com.portfolens.entity.User;
import com.portfolens.repository.HoldingRepository;
import com.portfolens.repository.OutboxEventRepository;
import com.portfolens.repository.PortfolioRepository;
import com.portfolens.repository.PortfolioTransactionRepository;
import com.portfolens.repository.StockRepository;
import com.portfolens.repository.UserRepository;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PortfolioTransactionOutboxTest {
    @Mock PortfolioRepository portfolios;
    @Mock UserRepository users;
    @Mock StockRepository stocks;
    @Mock HoldingRepository holdings;
    @Mock PortfolioTransactionRepository transactions;
    @Mock OutboxEventRepository outbox;

    @Test
    void persistsOneOutboxEventAndReturnsOriginalTransactionOnRetry() {
        UUID portfolioId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        UUID stockId = UUID.randomUUID();
        UUID transactionId = UUID.randomUUID();
        String owner = ownerId.toString();
        String key = "request-123";
        var request = new TransactionRequest(stockId, PortfolioTransaction.Type.BUY, BigDecimal.TEN, BigDecimal.valueOf(12), null);
        Portfolio portfolio = org.mockito.Mockito.mock(Portfolio.class);
        User user = org.mockito.Mockito.mock(User.class);
        Stock stock = org.mockito.Mockito.mock(Stock.class);
        PortfolioTransaction transaction = org.mockito.Mockito.mock(PortfolioTransaction.class);
        when(portfolio.getId()).thenReturn(portfolioId);
        when(portfolio.getUser()).thenReturn(user);
        when(user.getId()).thenReturn(ownerId);
        when(portfolios.lockByIdAndUserId(portfolioId, ownerId)).thenReturn(Optional.of(portfolio));
        when(stocks.findByIdAndActiveTrue(stockId)).thenReturn(Optional.of(stock));
        when(stock.getId()).thenReturn(stockId);
        when(stock.getSymbol()).thenReturn("RELIANCE");
        when(stock.getCompanyName()).thenReturn("Reliance Industries");
        when(holdings.findByPortfolioIdAndStockId(portfolioId, stockId)).thenReturn(Optional.empty());
        when(transactions.findByPortfolioIdAndIdempotencyKey(portfolioId, key)).thenReturn(Optional.empty(), Optional.of(transaction));
        when(transactions.saveAndFlush(any())).thenReturn(transaction);
        when(transaction.getId()).thenReturn(transactionId);
        when(transaction.getPortfolio()).thenReturn(portfolio);
        when(transaction.getStock()).thenReturn(stock);
        when(transaction.getTransactionType()).thenReturn(PortfolioTransaction.Type.BUY);
        when(transaction.getQuantity()).thenReturn(BigDecimal.TEN);
        when(transaction.getPrice()).thenReturn(BigDecimal.valueOf(12));
        when(transaction.getTransactionDate()).thenReturn(java.time.Instant.parse("2025-01-01T00:00:00Z"));
        when(transaction.getCreatedAt()).thenReturn(java.time.Instant.parse("2025-01-01T00:00:01Z"));

        var service = new PortfolioService(portfolios, users, stocks, holdings, transactions, outbox,
                new ObjectMapper().findAndRegisterModules());
        var first = service.recordTransaction(portfolioId, owner, request, key);
        var retry = service.recordTransaction(portfolioId, owner, request, key);

        assertEquals(transactionId, first.id());
        assertEquals(first.id(), retry.id());
        verify(outbox).save(any());
        verify(transactions).saveAndFlush(any());
    }
}
