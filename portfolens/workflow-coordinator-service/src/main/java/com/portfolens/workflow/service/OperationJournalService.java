package com.portfolens.workflow.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.portfolens.workflow.domain.StepStatus;
import com.portfolens.workflow.domain.WorkflowEventType;
import com.portfolens.workflow.domain.WorkflowExecutionLog;
import com.portfolens.workflow.domain.WorkflowInstance;
import com.portfolens.workflow.domain.WorkflowStatus;
import com.portfolens.workflow.domain.WorkflowStep;
import com.portfolens.workflow.dto.CreateWorkflowRequest;
import com.portfolens.workflow.dto.WorkflowDtos;
import com.portfolens.workflow.exception.IdempotencyConflictException;
import com.portfolens.workflow.exception.InvalidStepStateException;
import com.portfolens.workflow.exception.WorkflowNotFoundException;
import com.portfolens.workflow.repository.WorkflowExecutionLogRepository;
import com.portfolens.workflow.repository.WorkflowInstanceRepository;
import com.portfolens.workflow.repository.WorkflowStepRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class OperationJournalService {
    private static final Logger log = LoggerFactory.getLogger(OperationJournalService.class);
    private final WorkflowInstanceRepository workflows;
    private final WorkflowStepRepository steps;
    private final WorkflowExecutionLogRepository logs;
    private final ObjectMapper mapper;
    private PlatformTransactionManager transactionManager;

    public OperationJournalService(WorkflowInstanceRepository workflows, WorkflowStepRepository steps,
            WorkflowExecutionLogRepository logs, ObjectMapper mapper) {
        this.workflows = workflows; this.steps = steps; this.logs = logs; this.mapper = mapper;
    }
    @Autowired(required = false)
    void setTransactionManager(PlatformTransactionManager transactionManager) { this.transactionManager = transactionManager; }

    public WorkflowInstance createWorkflow(String idempotencyKey, CreateWorkflowRequest request) {
        return createWorkflow(idempotencyKey, request, null);
    }
    public WorkflowInstance createWorkflow(String idempotencyKey, CreateWorkflowRequest request, UUID ownerUserId) {
        try {
            if (transactionManager == null) return createWorkflowTransaction(idempotencyKey, request, ownerUserId);
            return new TransactionTemplate(transactionManager).execute(status -> createWorkflowTransaction(idempotencyKey, request, ownerUserId));
        }
        catch (DataIntegrityViolationException duplicate) {
            WorkflowInstance winner;
            try { winner = getByIdempotencyKey(idempotencyKey); }
            catch (IllegalStateException unresolved) { throw duplicate; }
            String fingerprint = json(Map.of("workflowType", request.workflowType(), "requestPayload", request.requestPayload(), "steps", request.steps()));
            return existingOrConflict(winner, fingerprint);
        }
    }

    @Transactional
    public WorkflowInstance createWorkflowTransaction(String idempotencyKey, CreateWorkflowRequest request) {
        return createWorkflowTransaction(idempotencyKey, request, null);
    }
    @Transactional
    public WorkflowInstance createWorkflowTransaction(String idempotencyKey, CreateWorkflowRequest request, UUID ownerUserId) {
        String requestPayload = json(request.requestPayload());
        String fingerprintPayload = json(Map.of("workflowType", request.workflowType(), "requestPayload", request.requestPayload(), "steps", request.steps()));
        var prior = workflows.findByIdempotencyKey(idempotencyKey);
        if (prior.isPresent()) return existingOrConflict(prior.get(), fingerprintPayload);
        if (request.steps().stream().map(CreateWorkflowRequest.StepDefinition::stepName).distinct().count() != request.steps().size()
                || request.steps().stream().map(CreateWorkflowRequest.StepDefinition::stepOrder).distinct().count() != request.steps().size())
            throw new IllegalArgumentException("Step names and step orders must be unique within a workflow");
        UUID workflowId = UUID.randomUUID();
        WorkflowInstance workflow = new WorkflowInstance(UUID.randomUUID(), workflowId, idempotencyKey,
                hex(hash(fingerprintPayload)), request.workflowType(), requestPayload, ownerUserId);
        workflows.saveAndFlush(workflow);
        logs.save(new WorkflowExecutionLog(workflow, null, WorkflowEventType.WORKFLOW_CREATED, null, WorkflowStatus.CREATED.name(), "Workflow created", null));
        for (CreateWorkflowRequest.StepDefinition definition : request.steps()) {
            WorkflowStep step = new WorkflowStep(workflow, definition.stepName(), definition.stepOrder(), definition.maxRetries());
            step.configure(definition.targetService(), definition.operationIdentifier(), definition.mandatory(),
                    definition.timeoutMillis(), definition.compensationOperation());
            steps.save(step);
            logs.save(new WorkflowExecutionLog(workflow, step.getStepId(), WorkflowEventType.STEP_CREATED, null, StepStatus.PENDING.name(), "Workflow step created", null));
        }
        log.info("Created workflow {} of type {}", workflowId, workflow.getWorkflowType());
        return workflow;
    }

    @Transactional(readOnly = true, propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
    public WorkflowInstance getByIdempotencyKey(String key) {
        return workflows.findByIdempotencyKey(key).orElseThrow(() -> new IllegalStateException("Duplicate idempotency key could not be resolved"));
    }

    private WorkflowInstance existingOrConflict(WorkflowInstance prior, String fingerprint) {
        if (!MessageDigest.isEqual(prior.getRequestHash().getBytes(StandardCharsets.UTF_8), hex(hash(fingerprint)).getBytes(StandardCharsets.UTF_8))) throw new IdempotencyConflictException();
        return prior;
    }

    @Transactional
    public WorkflowInstance startWorkflow(UUID id) {
        WorkflowInstance workflow = loadForTransition(id);
        WorkflowTransitionRules.workflow(workflow.getStatus(), WorkflowStatus.RUNNING);
        workflow.transition(WorkflowStatus.RUNNING);
        append(id, null, WorkflowEventType.WORKFLOW_STARTED, WorkflowStatus.CREATED.name(), WorkflowStatus.RUNNING.name(), "Workflow started", null);
        return workflow;
    }

    @Transactional
    public WorkflowStep startStep(UUID workflowId, String stepName) {
        WorkflowInstance workflow = loadForTransition(workflowId);
        requireWorkflowRunning(workflow);
        WorkflowStep step = steps.findForUpdate(workflowId, stepName).orElseThrow(() -> new InvalidStepStateException("Step not found: " + stepName));
        StepStatus previous = step.getStatus();
        WorkflowTransitionRules.step(step.getStatus(), StepStatus.RUNNING);
        step.transition(StepStatus.RUNNING); workflow.setCurrentStep(stepName);
        append(workflowId, step.getStepId(), WorkflowEventType.STEP_STARTED, previous.name(), StepStatus.RUNNING.name(), "Workflow step started", null);
        return step;
    }

    @Transactional
    public WorkflowStep completeStep(UUID workflowId, String stepName, Map<String, Object> resultMetadata) {
        WorkflowInstance workflow = loadForTransition(workflowId); requireWorkflowRunning(workflow);
        WorkflowStep step = loadStep(workflowId, stepName);
        WorkflowTransitionRules.step(step.getStatus(), StepStatus.COMPLETED);
        StepStatus previous = step.getStatus(); step.transition(StepStatus.COMPLETED);
        step.setResultPayload(resultMetadata == null ? null : json(resultMetadata));
        append(workflowId, step.getStepId(), WorkflowEventType.STEP_COMPLETED, previous.name(), StepStatus.COMPLETED.name(), "Workflow step completed", resultMetadata == null ? null : json(resultMetadata));
        return step;
    }

    @Transactional
    public WorkflowStep failStep(UUID workflowId, String stepName, String error) {
        return failStep(workflowId, stepName, error, "PERMANENT");
    }
    @Transactional
    public WorkflowStep failStep(UUID workflowId, String stepName, String error, String category) {
        WorkflowInstance workflow = loadForTransition(workflowId); requireWorkflowRunning(workflow);
        WorkflowStep step = loadStep(workflowId, stepName);
        WorkflowTransitionRules.step(step.getStatus(), StepStatus.FAILED);
        StepStatus previous = step.getStatus(); step.failed(error, category);
        append(workflowId, step.getStepId(), WorkflowEventType.STEP_FAILED, previous.name(), StepStatus.FAILED.name(), "Workflow step failed", json(Map.of("error", error == null ? "Unknown failure" : error)));
        return step;
    }

    @Transactional
    public WorkflowStep scheduleRetry(UUID workflowId, String stepName) {
        return scheduleRetry(workflowId, stepName, Instant.now());
    }
    @Transactional
    public WorkflowStep scheduleRetry(UUID workflowId, String stepName, Instant nextAt) {
        WorkflowInstance workflow = loadForTransition(workflowId); requireWorkflowRunning(workflow);
        WorkflowStep step = loadStep(workflowId, stepName);
        if (step.getStatus() != StepStatus.FAILED) throw new InvalidStepStateException("Only failed steps can be scheduled for retry");
        if (step.getRetryCount() >= step.getMaxRetries()) throw new InvalidStepStateException("Retry limit reached for step " + stepName);
        StepStatus previous = step.getStatus(); step.retry(nextAt);
        append(workflowId, step.getStepId(), WorkflowEventType.RETRY_SCHEDULED, previous.name(), StepStatus.RETRY_PENDING.name(), "Retry scheduled", json(Map.of("retryCount", step.getRetryCount(), "maxRetries", step.getMaxRetries(), "nextRetryAt", nextAt.toString())));
        return step;
    }

    @Transactional
    public WorkflowStep prepareManualRetry(UUID workflowId, String stepName) {
        WorkflowInstance workflow = loadForTransition(workflowId); requireWorkflowRunning(workflow);
        WorkflowStep step = loadStep(workflowId, stepName);
        if (step.getStatus() != StepStatus.FAILED || !("AMBIGUOUS".equals(step.getFailureCategory()) || "RETRYABLE".equals(step.getFailureCategory())))
            throw new InvalidStepStateException("Only an ambiguous or retryable remote outcome can be manually retried");
        WorkflowTransitionRules.step(StepStatus.FAILED, StepStatus.RETRY_PENDING);
        step.manualRetry(Instant.now());
        append(workflowId, step.getStepId(), WorkflowEventType.RETRY_SCHEDULED, StepStatus.FAILED.name(), StepStatus.RETRY_PENDING.name(), "Manual recovery retry scheduled using the stable downstream idempotency key", null);
        return step;
    }

    @Transactional
    public WorkflowStep ensureDownstreamKey(UUID workflowId, String stepName, String key) {
        WorkflowStep step = loadStep(workflowId, stepName); step.ensureDownstreamKey(key); return step;
    }

    @Transactional
    public WorkflowInstance completeWorkflow(UUID id, Map<String, Object> result) {
        WorkflowInstance workflow = loadForTransition(id);
        WorkflowTransitionRules.workflow(workflow.getStatus(), WorkflowStatus.COMPLETED);
        List<WorkflowStep> all = steps.findByWorkflow_WorkflowIdOrderByStepOrder(id);
        if (all.stream().anyMatch(s -> s.isMandatory() && s.getStatus() != StepStatus.COMPLETED && s.getStatus() != StepStatus.SKIPPED))
            throw new InvalidStepStateException("Workflow cannot complete until every step is completed or skipped");
        workflow.complete(result == null ? null : json(result));
        append(id, null, WorkflowEventType.WORKFLOW_COMPLETED, WorkflowStatus.RUNNING.name(), WorkflowStatus.COMPLETED.name(), "Workflow completed", null);
        return workflow;
    }

    @Transactional
    public WorkflowInstance failWorkflow(UUID id, String message) {
        WorkflowInstance workflow = loadForTransition(id);
        WorkflowTransitionRules.workflow(workflow.getStatus(), WorkflowStatus.FAILED);
        WorkflowStatus previous = workflow.getStatus(); workflow.transition(WorkflowStatus.FAILED);
        append(id, null, WorkflowEventType.WORKFLOW_FAILED, previous.name(), WorkflowStatus.FAILED.name(), message, null);
        return workflow;
    }

    @Transactional
    public WorkflowInstance markRecoveryRequired(UUID id, String message, Map<String, Object> metadata) {
        WorkflowInstance workflow = loadForTransition(id);
        WorkflowTransitionRules.workflow(workflow.getStatus(), WorkflowStatus.RECOVERY_REQUIRED);
        WorkflowStatus previous = workflow.getStatus(); workflow.transition(WorkflowStatus.RECOVERY_REQUIRED);
        append(id, null, WorkflowEventType.RECOVERY_REQUIRED, previous.name(), WorkflowStatus.RECOVERY_REQUIRED.name(), message,
                metadata == null ? null : json(metadata));
        return workflow;
    }

    @Transactional(readOnly = true)
    public WorkflowDtos.WorkflowView getWorkflow(UUID id) {
        WorkflowInstance workflow = workflows.findByWorkflowId(id).orElseThrow(() -> new WorkflowNotFoundException(id));
        return WorkflowDtos.WorkflowView.from(workflow, steps.findByWorkflow_WorkflowIdOrderByStepOrder(id));
    }
    @Transactional(readOnly = true)
    public List<WorkflowDtos.StepView> getSteps(UUID id) { requireWorkflow(id); return steps.findByWorkflow_WorkflowIdOrderByStepOrder(id).stream().map(WorkflowDtos.StepView::from).toList(); }
    @Transactional(readOnly = true)
    public Page<WorkflowDtos.LogView> getLogs(UUID id, Pageable pageable) { requireWorkflow(id); return logs.findByWorkflow_WorkflowIdOrderByCreatedAtAscIdAsc(id, pageable).map(WorkflowDtos.LogView::from); }
    @Transactional(readOnly = true)
    public Page<WorkflowDtos.WorkflowView> list(WorkflowStatus status, String workflowType, Pageable pageable) {
        Page<WorkflowInstance> page = status != null && workflowType != null ? workflows.findByStatusAndWorkflowType(status, workflowType, pageable)
                : status != null ? workflows.findByStatus(status, pageable)
                : workflowType != null ? workflows.findByWorkflowType(workflowType, pageable) : workflows.findAll(pageable);
        return page.map(w -> WorkflowDtos.WorkflowView.from(w, steps.findByWorkflow_WorkflowIdOrderByStepOrder(w.getWorkflowId())));
    }
    @Transactional(readOnly = true)
    public Page<WorkflowDtos.WorkflowView> list(UUID ownerUserId, WorkflowStatus status, String workflowType, Pageable pageable) {
        Page<WorkflowInstance> page = status != null && workflowType != null ? workflows.findByOwnerUserIdAndStatusAndWorkflowType(ownerUserId, status, workflowType, pageable)
                : status != null ? workflows.findByOwnerUserIdAndStatus(ownerUserId, status, pageable)
                : workflowType != null ? workflows.findByOwnerUserIdAndWorkflowType(ownerUserId, workflowType, pageable)
                : workflows.findByOwnerUserId(ownerUserId, pageable);
        return page.map(w -> WorkflowDtos.WorkflowView.from(w, steps.findByWorkflow_WorkflowIdOrderByStepOrder(w.getWorkflowId())));
    }
    @Transactional(readOnly = true)
    public List<WorkflowInstance> findIncomplete() { return workflows.findIncomplete(List.of(WorkflowStatus.CREATED, WorkflowStatus.RUNNING, WorkflowStatus.COMPENSATING, WorkflowStatus.RECOVERY_REQUIRED)); }
    @Transactional(readOnly = true)
    public List<WorkflowInstance> findStaleRunning(Duration age) { return workflows.findByStatusAndUpdatedAtBefore(WorkflowStatus.RUNNING, Instant.now().minus(age)); }
    @Transactional(readOnly = true)
    public List<WorkflowStep> findFailedSteps() { return steps.findByStatus(StepStatus.FAILED); }
    @Transactional(readOnly = true)
    public List<WorkflowStep> findDueRetries() { return steps.findByStatusAndNextRetryAtBefore(StepStatus.RETRY_PENDING, Instant.now().plusMillis(1)); }
    @Transactional(readOnly = true)
    public List<WorkflowStep> getRunningSteps(UUID workflowId) { return steps.findByWorkflow_WorkflowIdAndStatusOrderByStepOrder(workflowId, StepStatus.RUNNING); }
    @Transactional(readOnly = true)
    public WorkflowStep getStep(UUID workflowId, String stepName) { return steps.findByWorkflow_WorkflowIdAndStepName(workflowId, stepName).orElseThrow(() -> new InvalidStepStateException("Step not found: " + stepName)); }
    @Transactional(readOnly = true)
    public WorkflowInstance getInstance(UUID workflowId) { return requireWorkflow(workflowId); }
    @Transactional
    public WorkflowInstance resumeRecovery(UUID id) {
        WorkflowInstance workflow = loadForTransition(id);
        WorkflowTransitionRules.workflow(workflow.getStatus(), WorkflowStatus.RUNNING);
        WorkflowStatus previous = workflow.getStatus(); workflow.transition(WorkflowStatus.RUNNING);
        append(id, null, WorkflowEventType.WORKFLOW_STARTED, previous.name(), WorkflowStatus.RUNNING.name(), "Workflow recovery resumed", null);
        return workflow;
    }
    @Transactional
    public boolean acquireLease(UUID id, String owner, Instant now, Instant until) {
        WorkflowInstance workflow = workflows.findByWorkflowIdForUpdate(id).orElse(null);
        if (workflow == null || (workflow.getLeaseOwner() != null && workflow.getLeaseExpiresAt() != null && workflow.getLeaseExpiresAt().isAfter(now))) return false;
        workflow.lease(owner, until); return true;
    }
    @Transactional
    public boolean renewLease(UUID id, String owner, Instant until) {
        WorkflowInstance workflow = workflows.findByWorkflowIdForUpdate(id).orElse(null);
        if (workflow == null || !owner.equals(workflow.getLeaseOwner())) return false;
        workflow.lease(owner, until); return true;
    }
    @Transactional
    public void releaseLease(UUID id, String owner) {
        WorkflowInstance workflow = workflows.findByWorkflowIdForUpdate(id).orElse(null);
        if (workflow != null && owner.equals(workflow.getLeaseOwner())) workflow.clearLease();
    }

    private WorkflowInstance loadForTransition(UUID id) {
        return workflows.findByWorkflowIdForUpdate(id).orElseThrow(() -> new WorkflowNotFoundException(id));
    }
    private WorkflowInstance requireWorkflow(UUID id) { return workflows.findByWorkflowId(id).orElseThrow(() -> new WorkflowNotFoundException(id)); }
    private WorkflowStep loadStep(UUID workflowId, String name) { return steps.findForUpdate(workflowId, name).orElseThrow(() -> new InvalidStepStateException("Step not found: " + name)); }
    private void requireWorkflowRunning(WorkflowInstance workflow) {
        if (workflow.getStatus() != WorkflowStatus.RUNNING) throw new com.portfolens.workflow.exception.InvalidWorkflowStateException("Workflow must be RUNNING to execute steps");
    }
    private void append(UUID workflow, UUID step, WorkflowEventType event, String previous, String next, String message, String metadata) {
        WorkflowInstance parent = requireWorkflow(workflow);
        logs.save(new WorkflowExecutionLog(parent, step, event, previous, next, message, metadata));
    }
    private String json(Object value) { try { return mapper.writeValueAsString(value); } catch (JsonProcessingException ex) { throw new IllegalArgumentException("Payload is not valid JSON", ex); } }
    private static byte[] hash(String value) { try { return MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)); } catch (NoSuchAlgorithmException ex) { throw new IllegalStateException(ex); } }
    private static String hex(byte[] value) { return java.util.HexFormat.of().formatHex(value); }
}
