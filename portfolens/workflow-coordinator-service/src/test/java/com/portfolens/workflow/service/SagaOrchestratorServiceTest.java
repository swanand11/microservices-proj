package com.portfolens.workflow.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doNothing;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.portfolens.workflow.config.SagaProperties;
import com.portfolens.workflow.domain.StepStatus;
import com.portfolens.workflow.domain.WorkflowInstance;
import com.portfolens.workflow.domain.WorkflowStep;
import com.portfolens.workflow.dto.StockTransactionSagaRequest;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class SagaOrchestratorServiceTest {
    @Test void successfulTransactionUsesStableDownstreamKeyAndCompletesJournal() throws Exception {
        OperationJournalService journal = mock(OperationJournalService.class);
        PortfolioStepExecutor executor = mock(PortfolioStepExecutor.class);
        SagaProperties properties = new SagaProperties("http://portfolio", Duration.ofSeconds(1), Duration.ofSeconds(2),
                3, Duration.ofMillis(1), Duration.ofSeconds(1), 2, Duration.ofSeconds(5), Duration.ofMinutes(1));
        var saga = new SagaOrchestratorService(journal, executor, properties, new ObjectMapper(),
                new StockTransactionSagaDefinition(properties), new RetryPolicy(properties));
        UUID workflowId = UUID.randomUUID(), userId = UUID.randomUUID(), stepId = UUID.randomUUID();
        var workflow = new WorkflowInstance(UUID.randomUUID(), workflowId, "client-key", "0".repeat(64), "STOCK_TRANSACTION", "{}", userId);
        var validationStep = mock(WorkflowStep.class);
        when(validationStep.getStatus()).thenReturn(StepStatus.PENDING);
        var recordPending = mock(WorkflowStep.class);
        when(recordPending.getStatus()).thenReturn(StepStatus.PENDING);
        when(recordPending.getStepId()).thenReturn(stepId);
        var recordCompleted = mock(WorkflowStep.class);
        when(recordCompleted.getStatus()).thenReturn(StepStatus.COMPLETED);
        when(recordCompleted.getResultPayload()).thenReturn("{\"transactionId\":\"txn-1\"}");
        when(journal.createWorkflow(any(), any(), eq(userId))).thenReturn(workflow);
        when(journal.acquireLease(eq(workflowId), any(), any(), any())).thenReturn(true);
        when(journal.renewLease(eq(workflowId), any(), any())).thenReturn(true);
        when(journal.getStep(workflowId, "validate-portfolio")).thenReturn(validationStep);
        when(journal.getStep(workflowId, "record-transaction")).thenReturn(recordPending, recordCompleted);
        when(journal.ensureDownstreamKey(eq(workflowId), eq("record-transaction"), any())).thenReturn(recordPending);
        when(executor.recordTransaction(any(), any(), any(), any())).thenReturn(new ObjectMapper().readTree("{\"id\":\"txn-1\"}"));
        doNothing().when(journal).releaseLease(eq(workflowId), any());
        var request = new StockTransactionSagaRequest(UUID.randomUUID(), UUID.randomUUID(),
                StockTransactionSagaRequest.TransactionType.BUY, BigDecimal.TEN, BigDecimal.ONE, null);

        saga.startStockTransaction(request, "client-key", "Bearer caller-token", userId.toString());

        verify(executor).recordTransaction(eq(request), eq("Bearer caller-token"), eq(workflowId + ":" + stepId), eq(workflowId.toString()));
        verify(journal).completeWorkflow(eq(workflowId), any());
        verify(journal, never()).markRecoveryRequired(eq(workflowId), any(), any());
        verify(journal).releaseLease(eq(workflowId), any());
    }
}
