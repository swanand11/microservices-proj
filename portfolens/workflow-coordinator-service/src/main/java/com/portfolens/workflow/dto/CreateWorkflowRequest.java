package com.portfolens.workflow.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.Map;

public record CreateWorkflowRequest(
        @NotBlank @Size(max = 120) String workflowType,
        @NotNull Map<String, Object> requestPayload,
        @NotNull @Size(min = 1, max = 100) List<@Valid StepDefinition> steps) {
    public record StepDefinition(@NotBlank @Size(max = 160) String stepName,
            @NotNull @Min(0) Integer stepOrder, @NotNull @Min(0) Integer maxRetries,
            @Size(max = 120) String targetService, @Size(max = 200) String operationIdentifier,
            Boolean mandatory, @Min(1) Long timeoutMillis, @Size(max = 200) String compensationOperation) {
        public StepDefinition {
            if (mandatory == null) mandatory = true;
            if (timeoutMillis == null) timeoutMillis = 10_000L;
        }
        public StepDefinition(String stepName, Integer stepOrder, Integer maxRetries) {
            this(stepName, stepOrder, maxRetries, null, null, true, 10_000L, null);
        }
    }
}
