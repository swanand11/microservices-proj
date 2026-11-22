package com.portfolens.analytics.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDate;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class MarketDataClient {
    private final RestClient client;
    public MarketDataClient(@Value("${app.market-data-url}") String url,
            @Value("${app.market-data-service-key:}") String serviceKey) {
        var builder = RestClient.builder().baseUrl(url);
        if (serviceKey != null && !serviceKey.isBlank()) builder.defaultHeader("X-Internal-Service-Key", serviceKey);
        this.client = builder.build();
    }

    public List<StockPrice> stockHistory(String symbol) {
        StockHistory result = client.get().uri(uri -> uri.path("/api/market/stocks/{symbol}/history").queryParam("period", "1y").build(symbol))
                .retrieve().body(StockHistory.class);
        if (result == null || result.records() == null || result.records().isEmpty()) {
            client.post().uri("/api/market/download/{symbol}", symbol)
                    .body(java.util.Map.of("period", "1y", "refresh", false)).retrieve().toBodilessEntity();
            result = client.get().uri(uri -> uri.path("/api/market/stocks/{symbol}/history").queryParam("period", "1y").build(symbol))
                    .retrieve().body(StockHistory.class);
        }
        return result == null || result.records() == null ? List.of() : result.records();
    }
    public List<SeriesPoint> seriesHistory(String series) {
        SeriesHistory result = client.get().uri("/api/market/series/{series}/history?period=1y", series)
                .retrieve().body(SeriesHistory.class);
        return result == null || result.records() == null ? List.of() : result.records();
    }
    public double currentRiskFreeRate(String configuredFallback) {
        try {
            RiskFree result = client.get().uri("/api/market/series/gsec10y/latest").retrieve().body(RiskFree.class);
            if (result == null || !Double.isFinite(result.value())) throw new IllegalStateException("Empty G-Sec rate response");
            return result.value();
        } catch (RuntimeException failure) {
            if (configuredFallback == null || configuredFallback.isBlank()) throw failure;
            double fallback = Double.parseDouble(configuredFallback);
            return fallback > 1.0 ? fallback / 100.0 : fallback;
        }
    }

    public record StockHistory(List<StockPrice> records) {}
    public record StockPrice(LocalDate date, Double close, @JsonProperty("adjusted_close") Double adjustedClose) {}
    public record SeriesHistory(List<SeriesPoint> records) {}
    public record SeriesPoint(LocalDate date, double value) {}
    public record RiskFree(double value) {}
}
