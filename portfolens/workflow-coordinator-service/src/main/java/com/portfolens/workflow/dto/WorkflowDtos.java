package com.portfolens.workflow.dto;

import com.portfolens.workflow.domain.WorkflowExecutionLog;
import com.portfolens.workflow.domain.WorkflowInstance;
import com.portfolens.workflow.domain.WorkflowStep;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public final class WorkflowDtos {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private WorkflowDtos() {}
    public record StepView(UUID stepId, String stepName, int stepOrder, String status, int retryCount,
            int maxRetries, String compensationStatus, String targetService, String operationIdentifier,
            boolean mandatory, long timeoutMillis, String compensationOperation, String lastError, Instant startedAt,
            Instant completedAt, Instant createdAt, Instant updatedAt) {
        public static StepView from(WorkflowStep s) { return new StepView(s.getStepId(), s.getStepName(), s.getStepOrder(),
                s.getStatus().name(), s.getRetryCount(), s.getMaxRetries(), s.getCompensationStatus().name(),
                s.getTargetService(), s.getOperationIdentifier(), s.isMandatory(), s.getTimeoutMs(), s.getCompensationOperation(),
                s.getLastError(), s.getStartedAt(), s.getCompletedAt(), s.getCreatedAt(), s.getUpdatedAt()); }
    }
    public record WorkflowView(UUID workflowId, String workflowType, String status, JsonNode requestPayload,
            JsonNode resultPayload, String currentStep, Instant createdAt, Instant updatedAt,
            Instant completedAt, List<StepView> steps) {
        public static WorkflowView from(WorkflowInstance w, List<WorkflowStep> steps) { return new WorkflowView(
                w.getWorkflowId(), w.getWorkflowType(), w.getStatus().name(), parse(w.getRequestPayload()),
                parseNullable(w.getResultPayload()), w.getCurrentStep(), w.getCreatedAt(), w.getUpdatedAt(), w.getCompletedAt(),
                steps.stream().map(StepView::from).toList()); }
    }
    public record LogView(UUID id, UUID workflowId, UUID stepId, String eventType, String previousStatus,
            String newStatus, String message, JsonNode metadata, Instant createdAt) {
        public static LogView from(WorkflowExecutionLog l) { return new LogView(l.getId(), l.getWorkflowId(), l.getStepId(),
                l.getEventType().name(), l.getPreviousStatus(), l.getNewStatus(), l.getMessage(), parseNullable(l.getMetadata()), l.getCreatedAt()); }
    }
    private static JsonNode parseNullable(String value) { return value == null ? null : parse(value); }
    private static JsonNode parse(String value) {
        try { return MAPPER.readTree(value); }
        catch (com.fasterxml.jackson.core.JsonProcessingException ex) { throw new IllegalStateException("Stored journal JSON is invalid", ex); }
    }
}
