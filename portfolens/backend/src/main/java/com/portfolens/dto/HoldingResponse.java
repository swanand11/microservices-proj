package com.portfolens.dto;
import com.portfolens.entity.Holding;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
public record HoldingResponse(UUID id, UUID stockId, String symbol, String companyName, BigDecimal quantity, BigDecimal averageBuyPrice, Instant createdAt, Instant updatedAt) {
    public static HoldingResponse from(Holding h) { return new HoldingResponse(h.getId(), h.getStock().getId(), h.getStock().getSymbol(), h.getStock().getCompanyName(), h.getQuantity(), h.getAverageBuyPrice(), h.getCreatedAt(), h.getUpdatedAt()); }
}
