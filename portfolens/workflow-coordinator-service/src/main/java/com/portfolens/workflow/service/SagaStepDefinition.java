package com.portfolens.workflow.service;

import java.time.Duration;

public record SagaStepDefinition(String name, int order, String targetService, String operation,
        boolean mandatory, int maxRetries, Duration timeout, String compensationOperation) {}
