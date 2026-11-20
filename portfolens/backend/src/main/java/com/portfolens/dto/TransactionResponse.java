package com.portfolens.dto;
import com.portfolens.entity.PortfolioTransaction;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
public record TransactionResponse(UUID id, UUID stockId, String symbol, String companyName, PortfolioTransaction.Type transactionType, BigDecimal quantity, BigDecimal price, Instant transactionDate, Instant createdAt) {
    public static TransactionResponse from(PortfolioTransaction t) { return new TransactionResponse(t.getId(), t.getStock().getId(), t.getStock().getSymbol(), t.getStock().getCompanyName(), t.getTransactionType(), t.getQuantity(), t.getPrice(), t.getTransactionDate(), t.getCreatedAt()); }
}
