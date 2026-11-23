package com.portfolens.workflow.service;

import com.portfolens.workflow.config.SagaProperties;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class StockTransactionSagaDefinition {
    private final SagaDefinition definition;
    public StockTransactionSagaDefinition(SagaProperties properties) {
        definition = new SagaDefinition("STOCK_TRANSACTION", List.of(
                new SagaStepDefinition("validate-portfolio", 1, "portfolio-service", "GET /api/portfolios/{id}", true, properties.maxRetries(), properties.responseTimeout(), null),
                new SagaStepDefinition("record-transaction", 2, "portfolio-service", "POST /api/portfolios/{id}/transactions", true, properties.maxRetries(), properties.responseTimeout(), null)));
    }
    public SagaDefinition definition() { return definition; }
}
