package com.portfolens.workflow.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.saga")
public record SagaProperties(String portfolioBaseUrl, Duration connectTimeout, Duration responseTimeout,
        int maxRetries, Duration initialRetryDelay, Duration maxRetryDelay, double retryMultiplier,
        Duration recoveryInterval, Duration leaseDuration) {}
