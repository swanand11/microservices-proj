package com.portfolens.analytics.messaging;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PortfolioTransactionRecorded(UUID eventId, UUID transactionId, UUID portfolioId, UUID ownerId,
        UUID stockId, String symbol, String transactionType, BigDecimal quantity, BigDecimal price,
        Instant transactionDate, Instant recordedAt) {}
