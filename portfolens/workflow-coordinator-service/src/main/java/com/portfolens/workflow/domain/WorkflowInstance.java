package com.portfolens.workflow.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "workflow_instances", indexes = {
    @Index(name = "idx_workflow_instances_status", columnList = "status"),
    @Index(name = "idx_workflow_instances_created_at", columnList = "created_at")
})
public class WorkflowInstance {
    @Id private UUID id;
    @Column(name = "workflow_id", nullable = false, unique = true) private UUID workflowId;
    @Column(nullable = false, unique = true, length = 255) private String idempotencyKey;
    @Column(nullable = false, length = 64) private String requestHash;
    @Column(nullable = false, length = 120) private String workflowType;
    @Column(name = "owner_user_id") private UUID ownerUserId;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 32) private WorkflowStatus status;
    @JdbcTypeCode(SqlTypes.JSON) @Column(nullable = false, columnDefinition = "jsonb") private String requestPayload;
    @JdbcTypeCode(SqlTypes.JSON) @Column(columnDefinition = "jsonb") private String resultPayload;
    @Column(length = 160) private String currentStep;
    @Column(nullable = false, updatable = false) private Instant createdAt;
    @Column(nullable = false) private Instant updatedAt;
    private Instant completedAt;
    @Column(length = 160) private String leaseOwner;
    private Instant leaseExpiresAt;
    @Version @Column(nullable = false) private long version;

    protected WorkflowInstance() {}
    public WorkflowInstance(UUID id, UUID workflowId, String idempotencyKey, String requestHash, String workflowType, String requestPayload) {
        this(id, workflowId, idempotencyKey, requestHash, workflowType, requestPayload, null);
    }
    public WorkflowInstance(UUID id, UUID workflowId, String idempotencyKey, String requestHash, String workflowType, String requestPayload, UUID ownerUserId) {
        this.id = id; this.workflowId = workflowId; this.idempotencyKey = idempotencyKey;
        this.workflowType = workflowType; this.requestPayload = requestPayload; this.requestHash = requestHash; this.ownerUserId = ownerUserId; this.status = WorkflowStatus.CREATED;
        this.createdAt = Instant.now(); this.updatedAt = createdAt;
    }
    public void transition(WorkflowStatus next) { this.status = next; this.updatedAt = Instant.now(); }
    public void setCurrentStep(String currentStep) { this.currentStep = currentStep; this.updatedAt = Instant.now(); }
    public void complete(String resultPayload) { this.status = WorkflowStatus.COMPLETED; this.resultPayload = resultPayload; this.completedAt = Instant.now(); this.updatedAt = completedAt; }
    public UUID getId() { return id; } public UUID getWorkflowId() { return workflowId; }
    public String getIdempotencyKey() { return idempotencyKey; } public String getWorkflowType() { return workflowType; }
    public UUID getOwnerUserId() { return ownerUserId; }
    public WorkflowStatus getStatus() { return status; } public String getRequestPayload() { return requestPayload; }
    public String getResultPayload() { return resultPayload; } public String getCurrentStep() { return currentStep; }
    public String getRequestHash() { return requestHash; }
    public Instant getCreatedAt() { return createdAt; } public Instant getUpdatedAt() { return updatedAt; }
    public Instant getCompletedAt() { return completedAt; } public long getVersion() { return version; }
    public String getLeaseOwner() { return leaseOwner; } public Instant getLeaseExpiresAt() { return leaseExpiresAt; }
    public void lease(String owner, Instant expiresAt) { leaseOwner = owner; leaseExpiresAt = expiresAt; }
    public void clearLease() { leaseOwner = null; leaseExpiresAt = null; }
}
