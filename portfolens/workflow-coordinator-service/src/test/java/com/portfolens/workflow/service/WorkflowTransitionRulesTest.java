package com.portfolens.workflow.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.portfolens.workflow.domain.StepStatus;
import com.portfolens.workflow.domain.WorkflowStatus;
import com.portfolens.workflow.exception.InvalidStepStateException;
import com.portfolens.workflow.exception.InvalidWorkflowStateException;
import org.junit.jupiter.api.Test;

class WorkflowTransitionRulesTest {
    @Test void acceptsDefinedTransitions() {
        assertDoesNotThrow(() -> WorkflowTransitionRules.workflow(WorkflowStatus.CREATED, WorkflowStatus.RUNNING));
        assertDoesNotThrow(() -> WorkflowTransitionRules.step(StepStatus.FAILED, StepStatus.RETRY_PENDING));
    }

    @Test void rejectsUndefinedTransitions() {
        assertThrows(InvalidWorkflowStateException.class,
                () -> WorkflowTransitionRules.workflow(WorkflowStatus.COMPLETED, WorkflowStatus.RUNNING));
        assertThrows(InvalidStepStateException.class,
                () -> WorkflowTransitionRules.step(StepStatus.PENDING, StepStatus.COMPLETED));
    }
}
