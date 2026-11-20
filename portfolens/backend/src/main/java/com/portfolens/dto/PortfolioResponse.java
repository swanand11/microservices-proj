package com.portfolens.dto;
import com.portfolens.entity.Portfolio;
import java.time.Instant;
import java.util.UUID;
public record PortfolioResponse(UUID id, String name, String description, Instant createdAt, Instant updatedAt) {
    public static PortfolioResponse from(Portfolio p) { return new PortfolioResponse(p.getId(), p.getName(), p.getDescription(), p.getCreatedAt(), p.getUpdatedAt()); }
}
