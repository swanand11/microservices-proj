package com.portfolens.workflow.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.ForeignKey;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "workflow_steps", uniqueConstraints = {
    @UniqueConstraint(name = "uq_workflow_steps_name", columnNames = {"workflow_id", "step_name"}),
    @UniqueConstraint(name = "uq_workflow_steps_order", columnNames = {"workflow_id", "step_order"})
}, indexes = @Index(name = "idx_workflow_steps_status", columnList = "status"))
public class WorkflowStep {
    @Id private UUID id;
    @Column(nullable = false, unique = true) private UUID stepId;
    @ManyToOne(fetch = jakarta.persistence.FetchType.LAZY, optional = false)
    @JoinColumn(name = "workflow_id", referencedColumnName = "workflow_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_workflow_steps_instance"))
    private WorkflowInstance workflow;
    @Column(nullable = false, length = 160) private String stepName;
    @Column(name = "step_order", nullable = false) private int stepOrder;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 32) private StepStatus status;
    @Column(nullable = false) private int retryCount;
    @Column(nullable = false) private int maxRetries;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 32) private CompensationStatus compensationStatus;
    @Column(columnDefinition = "text") private String lastError;
    private Instant startedAt;
    private Instant completedAt;
    private Instant nextRetryAt;
    private Instant lastAttemptAt;
    @Column(length = 255) private String downstreamIdempotencyKey;
    @Column(length = 40) private String failureCategory;
    @Column(nullable = false) private int compensationAttemptCount;
    @JdbcTypeCode(SqlTypes.JSON) @Column(columnDefinition = "jsonb") private String resultPayload;
    @Column(length = 120) private String targetService;
    @Column(length = 200) private String operationIdentifier;
    @Column(nullable = false) private boolean mandatory = true;
    @Column(nullable = false) private long timeoutMs = 10_000;
    @Column(length = 200) private String compensationOperation;
    @Column(nullable = false, updatable = false) private Instant createdAt;
    @Column(nullable = false) private Instant updatedAt;
    @Version @Column(nullable = false) private long version;

    protected WorkflowStep() {}
    public WorkflowStep(WorkflowInstance workflow, String stepName, int stepOrder, int maxRetries) {
        this.workflow = workflow;
        this.id = UUID.randomUUID(); this.stepId = UUID.randomUUID();
        this.stepName = stepName; this.stepOrder = stepOrder; this.maxRetries = maxRetries;
        this.status = StepStatus.PENDING; this.compensationStatus = CompensationStatus.NOT_REQUIRED;
        this.createdAt = Instant.now(); this.updatedAt = createdAt;
    }
    public void transition(StepStatus next) { this.status = next; this.updatedAt = Instant.now();
        if (next == StepStatus.RUNNING) { if (startedAt == null) startedAt = updatedAt; completedAt = null; lastAttemptAt = updatedAt; nextRetryAt = null; }
        if (next == StepStatus.COMPLETED || next == StepStatus.FAILED || next == StepStatus.SKIPPED) completedAt = updatedAt;
    }
    public void failed(String error, String category) { lastError = error; failureCategory = category; transition(StepStatus.FAILED); }
    public void retry(Instant nextAt) { retryCount++; nextRetryAt = nextAt; transition(StepStatus.RETRY_PENDING); }
    public void manualRetry(Instant nextAt) { nextRetryAt = nextAt; transition(StepStatus.RETRY_PENDING); }
    public void ensureDownstreamKey(String key) { if (downstreamIdempotencyKey == null) downstreamIdempotencyKey = key; }
    public void setResultPayload(String payload) { resultPayload = payload; }
    public void configure(String targetService, String operationIdentifier, boolean mandatory, long timeoutMs, String compensationOperation) {
        this.targetService = targetService; this.operationIdentifier = operationIdentifier; this.mandatory = mandatory;
        this.timeoutMs = timeoutMs; this.compensationOperation = compensationOperation;
    }
    public UUID getId() { return id; } public UUID getStepId() { return stepId; }
    public UUID getWorkflowId() { return workflow.getWorkflowId(); } public String getStepName() { return stepName; }
    public int getStepOrder() { return stepOrder; } public StepStatus getStatus() { return status; }
    public int getRetryCount() { return retryCount; } public int getMaxRetries() { return maxRetries; }
    public CompensationStatus getCompensationStatus() { return compensationStatus; }
    public String getLastError() { return lastError; } public Instant getStartedAt() { return startedAt; }
    public Instant getCompletedAt() { return completedAt; } public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; } public long getVersion() { return version; }
    public Instant getNextRetryAt() { return nextRetryAt; } public Instant getLastAttemptAt() { return lastAttemptAt; }
    public String getDownstreamIdempotencyKey() { return downstreamIdempotencyKey; }
    public String getFailureCategory() { return failureCategory; }
    public int getCompensationAttemptCount() { return compensationAttemptCount; }
    public String getResultPayload() { return resultPayload; }
    public String getTargetService() { return targetService; } public String getOperationIdentifier() { return operationIdentifier; }
    public boolean isMandatory() { return mandatory; } public long getTimeoutMs() { return timeoutMs; }
    public String getCompensationOperation() { return compensationOperation; }
}
