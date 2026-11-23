package com.portfolens.workflow.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "workflow_execution_log", indexes = @Index(name = "idx_workflow_execution_log_step", columnList = "step_id"))
public class WorkflowExecutionLog {
    @Id private UUID id;
    @ManyToOne(fetch = jakarta.persistence.FetchType.LAZY, optional = false)
    @JoinColumn(name = "workflow_id", referencedColumnName = "workflow_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_workflow_log_instance"))
    private WorkflowInstance workflow;
    @Column(name = "step_id") private UUID stepId;
    @Enumerated(EnumType.STRING) @Column(name = "event_type", nullable = false, length = 40) private WorkflowEventType eventType;
    @Column(name = "previous_status", length = 32) private String previousStatus;
    @Column(name = "new_status", length = 32) private String newStatus;
    @Column(nullable = false, columnDefinition = "text") private String message;
    @JdbcTypeCode(SqlTypes.JSON) @Column(columnDefinition = "jsonb") private String metadata;
    @Column(nullable = false, updatable = false) private Instant createdAt;

    protected WorkflowExecutionLog() {}
    public WorkflowExecutionLog(WorkflowInstance workflow, UUID stepId, WorkflowEventType eventType,
            String previousStatus, String newStatus, String message, String metadata) {
        this.id = UUID.randomUUID(); this.workflow = workflow; this.stepId = stepId;
        this.eventType = eventType; this.previousStatus = previousStatus; this.newStatus = newStatus;
        this.message = message; this.metadata = metadata; this.createdAt = Instant.now();
    }
    public UUID getId() { return id; } public UUID getWorkflowId() { return workflow.getWorkflowId(); }
    public UUID getStepId() { return stepId; } public WorkflowEventType getEventType() { return eventType; }
    public String getPreviousStatus() { return previousStatus; } public String getNewStatus() { return newStatus; }
    public String getMessage() { return message; } public String getMetadata() { return metadata; }
    public Instant getCreatedAt() { return createdAt; }
}
