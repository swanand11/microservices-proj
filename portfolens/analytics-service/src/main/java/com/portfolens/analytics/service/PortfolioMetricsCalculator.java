package com.portfolens.analytics.service;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class PortfolioMetricsCalculator {
    private static final int TRADING_DAYS = 252;
    private static final int MIN_OBSERVATIONS = 30;

    public PortfolioMetrics calculate(UUID portfolioId, List<DailyReturns> returns, double riskFreeAnnualRate) {
        List<DailyReturns> valid = returns.stream().filter(r -> Double.isFinite(r.portfolio()) && Double.isFinite(r.benchmark())).toList();
        LocalDate start = valid.isEmpty() ? null : valid.get(0).date();
        LocalDate end = valid.isEmpty() ? null : valid.get(valid.size() - 1).date();
        if (valid.size() < MIN_OBSERVATIONS) {
            return new PortfolioMetrics(portfolioId, "INSUFFICIENT_HISTORY", "BSE_SENSEX", start, end, valid.size(), null, null, null, null,
                    riskFreeAnnualRate, "At least 30 aligned daily return observations are required.", Instant.now());
        }

        double dailyRiskFree = Math.pow(1.0 + riskFreeAnnualRate, 1.0 / TRADING_DAYS) - 1.0;
        double meanPortfolio = mean(valid.stream().mapToDouble(DailyReturns::portfolio).toArray());
        double meanBenchmark = mean(valid.stream().mapToDouble(DailyReturns::benchmark).toArray());
        double covariance = covariance(valid, meanPortfolio, meanBenchmark);
        double benchmarkVariance = variance(valid.stream().mapToDouble(DailyReturns::benchmark).toArray(), meanBenchmark);
        if (benchmarkVariance <= 0.0) {
            return new PortfolioMetrics(portfolioId, "UNDEFINED", "BSE_SENSEX", start, end, valid.size(), null, null, null, null,
                    riskFreeAnnualRate, "Beta is undefined because benchmark returns have no variance.", Instant.now());
        }

        double beta = covariance / benchmarkVariance;
        double alpha = TRADING_DAYS * ((meanPortfolio - dailyRiskFree) - beta * (meanBenchmark - dailyRiskFree));
        double[] excess = valid.stream().mapToDouble(r -> r.portfolio() - dailyRiskFree).toArray();
        double excessMean = mean(excess);
        double excessStddev = Math.sqrt(variance(excess, excessMean));
        Double sharpe = excessStddev == 0.0 ? null : Math.sqrt(TRADING_DAYS) * excessMean / excessStddev;
        return new PortfolioMetrics(portfolioId, "READY", "BSE_SENSEX", start, end, valid.size(),
                covariance * TRADING_DAYS, beta, alpha, sharpe, riskFreeAnnualRate,
                sharpe == null ? "Sharpe ratio is undefined because portfolio excess returns have no variance." : null,
                Instant.now());
    }

    private double mean(double[] values) { return java.util.Arrays.stream(values).average().orElse(0.0); }
    private double variance(double[] values, double mean) {
        if (values.length < 2) return 0.0;
        double sum = 0.0;
        for (double value : values) sum += Math.pow(value - mean, 2);
        return sum / (values.length - 1);
    }
    private double covariance(List<DailyReturns> values, double meanPortfolio, double meanBenchmark) {
        double sum = 0.0;
        for (DailyReturns value : values) sum += (value.portfolio() - meanPortfolio) * (value.benchmark() - meanBenchmark);
        return sum / (values.size() - 1);
    }
}
