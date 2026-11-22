package com.portfolens.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record AnalyticsResponse(
        UUID portfolioId,
        String status,
        String benchmark,
        LocalDate windowStart,
        LocalDate windowEnd,
        int observations,
        Double annualizedCovariance,
        Double beta,
        Double annualizedAlpha,
        Double annualizedSharpe,
        Double riskFreeAnnualRate,
        String message,
        Instant computedAt) {}
