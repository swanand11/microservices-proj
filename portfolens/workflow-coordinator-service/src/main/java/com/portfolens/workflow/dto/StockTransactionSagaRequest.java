package com.portfolens.workflow.dto;

import com.portfolens.workflow.dto.StockTransactionSagaRequest.TransactionType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record StockTransactionSagaRequest(
        @NotNull UUID portfolioId,
        @NotNull UUID stockId,
        @NotNull TransactionType transactionType,
        @NotNull @DecimalMin("0.00000001") BigDecimal quantity,
        @NotNull @DecimalMin("0.0001") BigDecimal price,
        Instant transactionDate) {
    public enum TransactionType { BUY, SELL }
}
