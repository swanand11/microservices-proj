package com.portfolens.workflow.service;

import com.portfolens.workflow.config.SagaProperties;
import java.time.Duration;
import org.springframework.stereotype.Component;

@Component
public class RetryPolicy {
    private final SagaProperties properties;
    public RetryPolicy(SagaProperties properties) { this.properties = properties; }
    public Duration delay(int retryNumber) { return delay(retryNumber, 0.8 + Math.random() * 0.4); }
    Duration delay(int retryNumber, double jitter) {
        if (retryNumber < 0 || jitter < 0 || jitter > 2) throw new IllegalArgumentException("Retry delay inputs are invalid");
        double raw = properties.initialRetryDelay().toMillis() * Math.pow(properties.retryMultiplier(), retryNumber);
        long capped = Math.min((long) raw, properties.maxRetryDelay().toMillis());
        long result = Math.min(properties.maxRetryDelay().toMillis(), (long) (capped * jitter));
        return Duration.ofMillis(Math.max(1, result));
    }
}
