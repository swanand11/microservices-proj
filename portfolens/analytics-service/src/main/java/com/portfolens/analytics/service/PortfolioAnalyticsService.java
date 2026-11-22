package com.portfolens.analytics.service;

import com.portfolens.analytics.entity.AnalyticsTransaction;
import com.portfolens.analytics.entity.PortfolioAnalyticsSnapshot;
import com.portfolens.analytics.repository.AnalyticsTransactionRepository;
import com.portfolens.analytics.repository.PortfolioAnalyticsRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class PortfolioAnalyticsService {
    private final AnalyticsTransactionRepository transactions;
    private final PortfolioAnalyticsRepository snapshots;
    private final MarketDataClient marketData;
    private final PortfolioMetricsCalculator calculator;
    private final String riskFreeFallback;

    public PortfolioAnalyticsService(AnalyticsTransactionRepository transactions, PortfolioAnalyticsRepository snapshots,
            MarketDataClient marketData, PortfolioMetricsCalculator calculator,
            @Value("${app.risk-free-annual-rate:}") String riskFreeFallback) {
        this.transactions = transactions; this.snapshots = snapshots; this.marketData = marketData;
        this.calculator = calculator; this.riskFreeFallback = riskFreeFallback;
    }

    public PortfolioMetrics calculate(UUID portfolioId) {
        List<AnalyticsTransaction> ledger = transactions.findAllByPortfolioIdOrderByTransactionDateAscRecordedAtAsc(portfolioId);
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        LocalDate windowStart = today.minusYears(1);
        if (ledger.isEmpty()) return save(calculator.calculate(portfolioId, List.of(), riskFreeFallbackValueOrZero()));

        try {
            Map<String, TreeMap<LocalDate, Double>> pricesBySymbol = new HashMap<>();
            for (String symbol : ledger.stream().map(AnalyticsTransaction::getSymbol).distinct().toList()) {
                TreeMap<LocalDate, Double> prices = new TreeMap<>();
                for (MarketDataClient.StockPrice price : marketData.stockHistory(symbol)) {
                    Double value = price.adjustedClose() == null ? price.close() : price.adjustedClose();
                    if (price.date() != null && value != null && value > 0) prices.put(price.date(), value);
                }
                pricesBySymbol.put(symbol, prices);
            }
            TreeMap<LocalDate, Double> benchmark = new TreeMap<>();
            for (MarketDataClient.SeriesPoint point : marketData.seriesHistory("sensex")) {
                if (point.date() != null && point.value() > 0) benchmark.put(point.date(), point.value());
            }
            double riskFree = marketData.currentRiskFreeRate(riskFreeFallback);
            List<DailyReturns> returns = buildReturns(ledger, pricesBySymbol, benchmark, windowStart, today);
            return save(calculator.calculate(portfolioId, returns, riskFree));
        } catch (RuntimeException failure) {
            return save(new PortfolioMetrics(portfolioId, "DATA_UNAVAILABLE", "BSE_SENSEX", windowStart, today,
                    0, null, null, null, null, null,
                    "Required stock, Sensex, or G-Sec market data is unavailable. " + safeMessage(failure), Instant.now()));
        }
    }

    private List<DailyReturns> buildReturns(List<AnalyticsTransaction> ledger,
            Map<String, TreeMap<LocalDate, Double>> pricesBySymbol, TreeMap<LocalDate, Double> benchmark,
            LocalDate windowStart, LocalDate today) {
        List<DailyReturns> result = new ArrayList<>();
        Map<String, Double> quantities = new HashMap<>();
        int txIndex = 0;
        Double previousPortfolioValue = null;
        Double previousBenchmarkValue = null;
        double pendingCashFlow = 0.0;
        for (LocalDate date : benchmark.subMap(windowStart, true, today, true).keySet()) {
            while (txIndex < ledger.size() && !ledger.get(txIndex).getTransactionDate().atZone(ZoneOffset.UTC).toLocalDate().isAfter(date)) {
                AnalyticsTransaction transaction = ledger.get(txIndex++);
                double direction = "BUY".equals(transaction.getTransactionType()) ? 1.0 : -1.0;
                double quantity = transaction.getQuantity().doubleValue();
                double notional = quantity * transaction.getPrice().doubleValue();
                quantities.merge(transaction.getSymbol(), direction * quantity, Double::sum);
                pendingCashFlow += direction * notional;
            }

            double portfolioValue = 0.0;
            boolean complete = true;
            for (var holding : quantities.entrySet()) {
                if (Math.abs(holding.getValue()) < 1e-10) continue;
                TreeMap<LocalDate, Double> priceSeries = pricesBySymbol.get(holding.getKey());
                Map.Entry<LocalDate, Double> price = priceSeries == null ? null : priceSeries.floorEntry(date);
                if (price == null) { complete = false; break; }
                portfolioValue += holding.getValue() * price.getValue();
            }
            Double benchmarkValue = benchmark.get(date);
            if (!complete || benchmarkValue == null || portfolioValue <= 0) continue;
            if (previousPortfolioValue != null && previousBenchmarkValue != null && previousPortfolioValue > 0) {
                double portfolioReturn = (portfolioValue - previousPortfolioValue - pendingCashFlow) / previousPortfolioValue;
                double benchmarkReturn = benchmarkValue / previousBenchmarkValue - 1.0;
                if (Double.isFinite(portfolioReturn) && Double.isFinite(benchmarkReturn)) result.add(new DailyReturns(date, portfolioReturn, benchmarkReturn));
            }
            previousPortfolioValue = portfolioValue;
            previousBenchmarkValue = benchmarkValue;
            pendingCashFlow = 0.0;
        }
        return result;
    }

    private PortfolioMetrics save(PortfolioMetrics metrics) {
        snapshots.save(new PortfolioAnalyticsSnapshot(metrics));
        return metrics;
    }
    private double riskFreeFallbackValueOrZero() {
        if (riskFreeFallback == null || riskFreeFallback.isBlank()) return 0.0;
        double value = Double.parseDouble(riskFreeFallback);
        return value > 1.0 ? value / 100.0 : value;
    }
    private String safeMessage(RuntimeException failure) {
        String message = failure.getMessage();
        if (message == null || message.isBlank()) return failure.getClass().getSimpleName();
        return message.length() > 250 ? message.substring(0, 250) : message;
    }
}
