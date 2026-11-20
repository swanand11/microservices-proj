package com.portfolens.dto;
import com.portfolens.entity.PortfolioTransaction.Type;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
public record TransactionRequest(@NotNull UUID stockId, @NotNull Type transactionType, @NotNull @DecimalMin(value="0.00000001") BigDecimal quantity, @NotNull @DecimalMin(value="0.0001") BigDecimal price, Instant transactionDate) {}
