package com.portfolens.dto;
import com.portfolens.entity.Stock;
import java.time.Instant;
import java.util.UUID;
public record StockResponse(UUID id, String symbol, String companyName, String exchange, String bseScripCode, String sector, String industry, boolean active, Instant createdAt) {
    public static StockResponse from(Stock s) { return new StockResponse(s.getId(), s.getSymbol(), s.getCompanyName(), s.getExchange(), s.getBseScripCode(), s.getSector(), s.getIndustry(), s.isActive(), s.getCreatedAt()); }
}
