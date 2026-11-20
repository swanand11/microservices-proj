package com.portfolens.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public final class MarketDtos {
    private MarketDtos() {}
    public record PriceRecord(LocalDate date, BigDecimal open, BigDecimal high, BigDecimal low, BigDecimal close, @JsonProperty("adjusted_close") BigDecimal adjustedClose, long volume) {}
    public record PythonHistory(String symbol, String company_name, String yahoo_ticker, String exchange, LocalDate data_start_date, LocalDate data_end_date, Instant last_updated, String source, String price_policy, long total_records, int offset, int limit, List<PriceRecord> records) {}
    public record PriceResponse(String symbol, String company_name, LocalDate trading_date, BigDecimal open, BigDecimal high, BigDecimal low, BigDecimal close, BigDecimal adjusted_close, long volume, String source, Instant fetched_at) {}
    public record SyncRequest(@Min(1) @Max(50) Integer limit) {}
    public record SyncResult(String symbol, int imported, LocalDate start_date, LocalDate end_date, Instant fetched_at) {}
    public record SyncStatus(String symbol, long stored_records, LocalDate latest_date, Instant last_fetched_at) {}
}
