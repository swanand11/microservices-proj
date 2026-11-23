package com.portfolens.workflow.service;

import java.util.List;

public record SagaDefinition(String workflowType, List<SagaStepDefinition> steps) {
    public SagaDefinition { steps = steps.stream().sorted(java.util.Comparator.comparingInt(SagaStepDefinition::order)).toList(); }
}
