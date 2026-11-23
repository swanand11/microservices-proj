package com.portfolens.workflow.service;

import com.portfolens.workflow.domain.StepStatus;
import com.portfolens.workflow.domain.WorkflowStatus;
import com.portfolens.workflow.exception.InvalidStepStateException;
import com.portfolens.workflow.exception.InvalidWorkflowStateException;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;

final class WorkflowTransitionRules {
    private static final Map<WorkflowStatus, EnumSet<WorkflowStatus>> WORKFLOW = new EnumMap<>(WorkflowStatus.class);
    private static final Map<StepStatus, EnumSet<StepStatus>> STEP = new EnumMap<>(StepStatus.class);
    static {
        WORKFLOW.put(WorkflowStatus.CREATED, EnumSet.of(WorkflowStatus.RUNNING, WorkflowStatus.FAILED, WorkflowStatus.RECOVERY_REQUIRED));
        WORKFLOW.put(WorkflowStatus.RUNNING, EnumSet.of(WorkflowStatus.COMPLETED, WorkflowStatus.FAILED, WorkflowStatus.COMPENSATING, WorkflowStatus.RECOVERY_REQUIRED));
        WORKFLOW.put(WorkflowStatus.FAILED, EnumSet.of(WorkflowStatus.COMPENSATING, WorkflowStatus.RECOVERY_REQUIRED));
        WORKFLOW.put(WorkflowStatus.COMPENSATING, EnumSet.of(WorkflowStatus.COMPENSATED, WorkflowStatus.FAILED, WorkflowStatus.RECOVERY_REQUIRED));
        WORKFLOW.put(WorkflowStatus.RECOVERY_REQUIRED, EnumSet.of(WorkflowStatus.RUNNING, WorkflowStatus.COMPENSATING));
        STEP.put(StepStatus.PENDING, EnumSet.of(StepStatus.RUNNING, StepStatus.SKIPPED));
        STEP.put(StepStatus.RUNNING, EnumSet.of(StepStatus.COMPLETED, StepStatus.FAILED));
        STEP.put(StepStatus.FAILED, EnumSet.of(StepStatus.RETRY_PENDING));
        STEP.put(StepStatus.RETRY_PENDING, EnumSet.of(StepStatus.RUNNING));
    }
    static void workflow(WorkflowStatus from, WorkflowStatus to) {
        if (!WORKFLOW.getOrDefault(from, EnumSet.noneOf(WorkflowStatus.class)).contains(to))
            throw new InvalidWorkflowStateException("Workflow cannot transition from " + from + " to " + to);
    }
    static void step(StepStatus from, StepStatus to) {
        if (!STEP.getOrDefault(from, EnumSet.noneOf(StepStatus.class)).contains(to))
            throw new InvalidStepStateException("Step cannot transition from " + from + " to " + to);
    }
}
