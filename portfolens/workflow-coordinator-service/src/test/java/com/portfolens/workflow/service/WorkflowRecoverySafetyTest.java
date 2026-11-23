package com.portfolens.workflow.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.portfolens.workflow.config.SagaProperties;
import com.portfolens.workflow.domain.WorkflowInstance;
import com.portfolens.workflow.domain.WorkflowStatus;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class WorkflowRecoverySafetyTest {
    @Test void staleCreatedWorkflowIsFlaggedWithoutCallingPortfolio() {
        OperationJournalService journal = mock(OperationJournalService.class);
        PortfolioStepExecutor executor = mock(PortfolioStepExecutor.class);
        SagaProperties properties = new SagaProperties("http://portfolio", Duration.ofSeconds(1), Duration.ofSeconds(2),
                3, Duration.ofMillis(1), Duration.ofSeconds(1), 2, Duration.ofSeconds(5), Duration.ZERO);
        var saga = new SagaOrchestratorService(journal, executor, properties, new ObjectMapper(), new StockTransactionSagaDefinition(properties), new RetryPolicy(properties));
        WorkflowInstance workflow = new WorkflowInstance(UUID.randomUUID(), UUID.randomUUID(), "key", "0".repeat(64), "STOCK_TRANSACTION", "{}", UUID.randomUUID());
        when(journal.findIncomplete()).thenReturn(List.of(workflow));
        when(journal.acquireLease(eq(workflow.getWorkflowId()), any(), any(), any())).thenReturn(true);
        when(journal.getInstance(workflow.getWorkflowId())).thenReturn(workflow);

        saga.recoverIncompleteWithoutCredentials();

        verify(journal).markRecoveryRequired(eq(workflow.getWorkflowId()), any(), any());
        verify(executor, never()).recordTransaction(any(), any(), any(), any());
    }
}
