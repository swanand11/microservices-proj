package com.portfolens.dto;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.UUID;
public record HoldingRequest(@NotNull UUID stockId, @NotNull @DecimalMin(value="0.00000001") BigDecimal quantity, @NotNull @DecimalMin(value="0.0001") BigDecimal averageBuyPrice) {}
