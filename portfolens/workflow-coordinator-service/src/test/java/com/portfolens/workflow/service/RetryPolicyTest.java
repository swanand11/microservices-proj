package com.portfolens.workflow.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.portfolens.workflow.config.SagaProperties;
import java.time.Duration;
import org.junit.jupiter.api.Test;

class RetryPolicyTest {
    private final RetryPolicy policy = new RetryPolicy(new SagaProperties("http://localhost", Duration.ofSeconds(3),
            Duration.ofSeconds(10), 3, Duration.ofSeconds(1), Duration.ofSeconds(30), 2.0,
            Duration.ofSeconds(5), Duration.ofMinutes(2)));

    @Test void exponentialDelayUsesMultiplierAndJitter() {
        assertEquals(Duration.ofMillis(1000), policy.delay(0, 1.0));
        assertEquals(Duration.ofMillis(2000), policy.delay(1, 1.0));
        assertEquals(Duration.ofMillis(4000), policy.delay(2, 1.0));
    }

    @Test void delayNeverExceedsConfiguredMaximum() {
        assertEquals(Duration.ofSeconds(30), policy.delay(20, 1.5));
    }
}
