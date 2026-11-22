package com.portfolens.analytics.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PortfolioMetricsCalculatorTest {
    private final PortfolioMetricsCalculator calculator = new PortfolioMetricsCalculator();

    @Test
    void calculatesBetaAndAnnualizedCovarianceFromAlignedDailyReturns() {
        List<DailyReturns> returns = java.util.stream.IntStream.range(0, 40)
                .mapToObj(index -> {
                    double market = (index % 2 == 0 ? 0.01 : -0.008) + (index % 5) * 0.0002;
                    return new DailyReturns(LocalDate.of(2025, 1, 1).plusDays(index), 0.001 + 1.5 * market, market);
                }).toList();
        var result = calculator.calculate(UUID.randomUUID(), returns, 0.07);
        assertEquals("READY", result.status());
        assertEquals(1.5, result.beta(), 1e-10);
        assertTrue(result.annualizedCovariance() > 0);
        assertTrue(result.annualizedSharpe() != null);
    }

    @Test
    void marksShortHistoryAsInsufficient() {
        var returns = java.util.stream.IntStream.range(0, 29)
                .mapToObj(index -> new DailyReturns(LocalDate.of(2025, 1, 1).plusDays(index), 0.01, 0.005)).toList();
        var result = calculator.calculate(UUID.randomUUID(), returns, 0.07);
        assertEquals("INSUFFICIENT_HISTORY", result.status());
        assertNull(result.beta());
        assertEquals(29, result.observations());
    }
}
