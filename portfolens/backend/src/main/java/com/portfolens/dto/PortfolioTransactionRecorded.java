package com.portfolens.dto;

import com.portfolens.entity.PortfolioTransaction;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PortfolioTransactionRecorded(
        UUID eventId,
        UUID transactionId,
        UUID portfolioId,
        UUID ownerId,
        UUID stockId,
        String symbol,
        String transactionType,
        BigDecimal quantity,
        BigDecimal price,
        Instant transactionDate,
        Instant recordedAt) {

    public static PortfolioTransactionRecorded from(PortfolioTransaction transaction, UUID eventId) {
        return new PortfolioTransactionRecorded(
                eventId,
                transaction.getId(),
                transaction.getPortfolio().getId(),
                transaction.getPortfolio().getUser().getId(),
                transaction.getStock().getId(),
                transaction.getStock().getSymbol(),
                transaction.getTransactionType().name(),
                transaction.getQuantity(),
                transaction.getPrice(),
                transaction.getTransactionDate(),
                transaction.getCreatedAt());
    }
}
