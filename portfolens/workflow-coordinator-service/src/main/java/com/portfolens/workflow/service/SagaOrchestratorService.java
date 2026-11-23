package com.portfolens.workflow.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.portfolens.workflow.config.SagaProperties;
import com.portfolens.workflow.domain.StepStatus;
import com.portfolens.workflow.domain.WorkflowInstance;
import com.portfolens.workflow.domain.WorkflowStep;
import com.portfolens.workflow.dto.CreateWorkflowRequest;
import com.portfolens.workflow.dto.StockTransactionSagaRequest;
import com.portfolens.workflow.dto.WorkflowDtos;
import com.portfolens.workflow.exception.InvalidWorkflowStateException;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class SagaOrchestratorService {
    private static final Logger log = LoggerFactory.getLogger(SagaOrchestratorService.class);
    private static final String VALIDATE = "validate-portfolio";
    private static final String RECORD = "record-transaction";
    private final OperationJournalService journal;
    private final PortfolioStepExecutor executor;
    private final SagaProperties properties;
    private final StockTransactionSagaDefinition stockDefinition;
    private final RetryPolicy retryPolicy;
    private final ObjectMapper mapper;
    private final String instanceId = UUID.randomUUID().toString();

    public SagaOrchestratorService(OperationJournalService journal, PortfolioStepExecutor executor,
            SagaProperties properties, ObjectMapper mapper, StockTransactionSagaDefinition stockDefinition, RetryPolicy retryPolicy) {
        this.journal = journal; this.executor = executor; this.properties = properties; this.mapper = mapper; this.stockDefinition = stockDefinition; this.retryPolicy = retryPolicy;
        if (properties.maxRetries() < 0 || properties.retryMultiplier() < 1.0) throw new IllegalStateException("Saga retry settings are invalid");
    }

    public WorkflowDtos.WorkflowView startStockTransaction(StockTransactionSagaRequest request,
            String idempotencyKey, String bearerToken, String ownerUserId) {
        Map<String, Object> transaction = Map.of("stockId", request.stockId(), "transactionType", request.transactionType().name(),
                "quantity", request.quantity(), "price", request.price(), "transactionDate", request.transactionDate() == null ? "" : request.transactionDate().toString());
        Map<String, Object> payload = Map.of("ownerUserId", ownerUserId, "portfolioId", request.portfolioId().toString(), "transaction", transaction);
        SagaDefinition definition = stockDefinition.definition();
        var command = new CreateWorkflowRequest(definition.workflowType(), payload, definition.steps().stream()
                .map(s -> new CreateWorkflowRequest.StepDefinition(s.name(), s.order(), s.maxRetries(), s.targetService(),
                        s.operation(), s.mandatory(), s.timeout().toMillis(), s.compensationOperation())).toList());
        WorkflowInstance workflow = journal.createWorkflow(idempotencyKey, command, UUID.fromString(ownerUserId));
        if (!UUID.fromString(ownerUserId).equals(workflow.getOwnerUserId())) throw new org.springframework.security.access.AccessDeniedException("Workflow does not belong to the authenticated user");
        UUID workflowId = workflow.getWorkflowId();
        if (workflow.getStatus() == com.portfolens.workflow.domain.WorkflowStatus.COMPLETED
                || workflow.getStatus() == com.portfolens.workflow.domain.WorkflowStatus.FAILED
                || workflow.getStatus() == com.portfolens.workflow.domain.WorkflowStatus.COMPENSATED)
            return journal.getWorkflow(workflowId);
        if (!journal.acquireLease(workflowId, instanceId, Instant.now(), Instant.now().plus(properties.leaseDuration())))
            return journal.getWorkflow(workflowId);
        try {
            if (workflow.getStatus() == com.portfolens.workflow.domain.WorkflowStatus.CREATED) journal.startWorkflow(workflowId);
            else if (workflow.getStatus() == com.portfolens.workflow.domain.WorkflowStatus.RECOVERY_REQUIRED) journal.resumeRecovery(workflowId);
            executeWorkflow(workflowId, request, bearerToken);
        } finally { journal.releaseLease(workflowId, instanceId); }
        return journal.getWorkflow(workflowId);
    }

    public WorkflowDtos.WorkflowView recover(UUID workflowId, String bearerToken, String ownerUserId) {
        WorkflowDtos.WorkflowView view = journal.getWorkflow(workflowId);
        assertOwner(view.requestPayload(), ownerUserId);
        if (!UUID.fromString(ownerUserId).equals(journal.getInstance(workflowId).getOwnerUserId())) throw new org.springframework.security.access.AccessDeniedException("Workflow does not belong to the authenticated user");
        var status = journal.getInstance(workflowId).getStatus();
        if (status != com.portfolens.workflow.domain.WorkflowStatus.RUNNING && status != com.portfolens.workflow.domain.WorkflowStatus.RECOVERY_REQUIRED)
            throw new InvalidWorkflowStateException("Only RUNNING or RECOVERY_REQUIRED workflows can be recovered");
        if (!journal.acquireLease(workflowId, instanceId, Instant.now(), Instant.now().plus(properties.leaseDuration()))) return journal.getWorkflow(workflowId);
        try {
            WorkflowInstance current = journal.getInstance(workflowId);
            if (current.getStatus() == com.portfolens.workflow.domain.WorkflowStatus.RECOVERY_REQUIRED) journal.resumeRecovery(workflowId);
            StockTransactionSagaRequest request = requestFrom(view.requestPayload());
            executeWorkflow(workflowId, request, bearerToken);
        } finally { journal.releaseLease(workflowId, instanceId); }
        return journal.getWorkflow(workflowId);
    }

    public void recoverIncompleteWithoutCredentials() {
        for (WorkflowInstance workflow : journal.findIncomplete()) {
            if (workflow.getUpdatedAt().isAfter(Instant.now().minus(properties.leaseDuration()))) continue;
            if (workflow.getStatus() == com.portfolens.workflow.domain.WorkflowStatus.RUNNING
                    || workflow.getStatus() == com.portfolens.workflow.domain.WorkflowStatus.CREATED) {
                if (!journal.acquireLease(workflow.getWorkflowId(), instanceId, Instant.now(), Instant.now().plus(properties.leaseDuration()))) continue;
                try {
                    var latest = journal.getInstance(workflow.getWorkflowId());
                    if (latest.getStatus() == com.portfolens.workflow.domain.WorkflowStatus.CREATED
                            || latest.getStatus() == com.portfolens.workflow.domain.WorkflowStatus.RUNNING) {
                        journal.markRecoveryRequired(workflow.getWorkflowId(), "Recovery requires an authenticated caller because the user JWT is not persisted", Map.of("reason", "CALLER_CREDENTIAL_REQUIRED"));
                    }
                } catch (RuntimeException failure) {
                    log.warn("Could not mark workflow {} for controlled recovery", workflow.getWorkflowId(), failure);
                } finally { journal.releaseLease(workflow.getWorkflowId(), instanceId); }
            }
        }
    }

    private void executeWorkflow(UUID id, StockTransactionSagaRequest request, String token) {
        try {
            executeSimpleStep(id, VALIDATE, () -> executor.validatePortfolio(request.portfolioId(), token, id.toString()));
            executeTransactionStep(id, request, token);
            WorkflowStep recordStep = journal.getStep(id, RECORD);
            if (recordStep.getStatus() != StepStatus.COMPLETED) return;
            JsonNode result = parse(recordStep.getResultPayload());
            journal.completeWorkflow(id, Map.of("transactionId", result.path("transactionId").asText()));
        } catch (RemoteCallFailure failure) {
            if (failure.outcomeUncertain()) journal.markRecoveryRequired(id, failure.getMessage(), Map.of("reason", "REMOTE_OUTCOME_UNCERTAIN"));
            else journal.failWorkflow(id, failure.getMessage());
        }
    }

    private void executeSimpleStep(UUID id, String name, Runnable operation) {
        WorkflowStep step = journal.getStep(id, name);
        if (step.getStatus() == StepStatus.COMPLETED) return;
        if (step.getStatus() == StepStatus.FAILED) { journal.prepareManualRetry(id, name); step = journal.getStep(id, name); }
        if (step.getStatus() != StepStatus.RUNNING) journal.startStep(id, name);
        while (true) {
            try { renewLease(id); operation.run(); journal.completeStep(id, name, null); return; }
            catch (RemoteCallFailure failure) {
                String category = failure.outcomeUncertain() ? "AMBIGUOUS" : failure.retryable() ? "RETRYABLE" : "PERMANENT";
                journal.failStep(id, name, failure.getMessage(), category);
                WorkflowStep state = journal.getStep(id, name);
                if (!failure.retryable() || state.getRetryCount() >= properties.maxRetries()) throw failure;
                Instant retryAt = Instant.now().plus(retryPolicy.delay(state.getRetryCount()));
                journal.scheduleRetry(id, name, retryAt); sleepUntil(retryAt); journal.startStep(id, name);
            }
        }
    }

    private JsonNode executeTransactionStep(UUID id, StockTransactionSagaRequest request, String token) {
        WorkflowStep step = journal.getStep(id, RECORD);
        if (step.getStatus() == StepStatus.COMPLETED) return parse(step.getResultPayload());
        if (step.getStatus() == StepStatus.FAILED) { journal.prepareManualRetry(id, RECORD); step = journal.getStep(id, RECORD); }
        String stableKey = step.getDownstreamIdempotencyKey() == null ? id + ":" + step.getStepId() : step.getDownstreamIdempotencyKey();
        step = journal.ensureDownstreamKey(id, RECORD, stableKey);
        if (step.getStatus() != StepStatus.RUNNING) journal.startStep(id, RECORD);
        while (true) {
            try {
                renewLease(id);
                JsonNode result = executor.recordTransaction(request, token, stableKey, id.toString());
                journal.completeStep(id, RECORD, Map.of("transactionId", result == null ? "unknown" : result.path("id").asText()));
                return result;
            } catch (RemoteCallFailure failure) {
                String category = failure.outcomeUncertain() ? "AMBIGUOUS" : failure.retryable() ? "RETRYABLE" : "PERMANENT";
                journal.failStep(id, RECORD, failure.getMessage(), category);
                WorkflowStep state = journal.getStep(id, RECORD);
                if (!failure.retryable() || state.getRetryCount() >= properties.maxRetries()) throw failure;
                Instant retryAt = Instant.now().plus(retryPolicy.delay(state.getRetryCount()));
                journal.scheduleRetry(id, RECORD, retryAt);
                sleepUntil(retryAt);
                journal.startStep(id, RECORD);
            }
        }
    }

    private void renewLease(UUID id) {
        if (!journal.renewLease(id, instanceId, Instant.now().plus(properties.leaseDuration())))
            throw new IllegalStateException("Workflow recovery lease was lost");
    }
    private void sleepUntil(Instant at) {
        try { Thread.sleep(Math.max(1, Duration.between(Instant.now(), at).toMillis())); }
        catch (InterruptedException ex) { Thread.currentThread().interrupt(); throw new IllegalStateException("Saga retry interrupted", ex); }
    }
    public void assertOwner(JsonNode payload, String ownerId) {
        if (!ownerId.equals(payload.path("ownerUserId").asText())) throw new org.springframework.security.access.AccessDeniedException("Workflow does not belong to the authenticated user");
    }
    private StockTransactionSagaRequest requestFrom(JsonNode payload) {
        JsonNode tx = payload.path("transaction"); String date = tx.path("transactionDate").asText("");
        return new StockTransactionSagaRequest(UUID.fromString(payload.path("portfolioId").asText()), UUID.fromString(tx.path("stockId").asText()),
                StockTransactionSagaRequest.TransactionType.valueOf(tx.path("transactionType").asText()), mapper.convertValue(tx.path("quantity"), java.math.BigDecimal.class),
                mapper.convertValue(tx.path("price"), java.math.BigDecimal.class), date.isBlank() ? null : Instant.parse(date));
    }
    private JsonNode parse(String value) {
        if (value == null) return mapper.createObjectNode();
        try { return mapper.readTree(value); } catch (java.io.IOException ex) { throw new IllegalStateException("Stored Saga step result is invalid", ex); }
    }
}
