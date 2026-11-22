package com.portfolens.analytics.entity;

import com.portfolens.analytics.service.PortfolioMetrics;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "portfolio_analytics")
public class PortfolioAnalyticsSnapshot {
    @Id private UUID portfolioId;
    @Column(nullable = false, length = 32) private String status;
    @Column(nullable = false, length = 32) private String benchmark;
    @Column(nullable = false) private int observations;
    private Double annualizedCovariance;
    private Double beta;
    private Double annualizedAlpha;
    private Double annualizedSharpe;
    private Double riskFreeAnnualRate;
    @Column(length = 500) private String message;
    @Column(nullable = false) private Instant computedAt;
    protected PortfolioAnalyticsSnapshot() {}

    public PortfolioAnalyticsSnapshot(PortfolioMetrics metrics) {
        portfolioId = metrics.portfolioId(); status = metrics.status(); benchmark = metrics.benchmark();
        observations = metrics.observations(); annualizedCovariance = metrics.annualizedCovariance(); beta = metrics.beta();
        annualizedAlpha = metrics.annualizedAlpha(); annualizedSharpe = metrics.annualizedSharpe();
        riskFreeAnnualRate = metrics.riskFreeAnnualRate(); message = metrics.message(); computedAt = metrics.computedAt();
    }
}
