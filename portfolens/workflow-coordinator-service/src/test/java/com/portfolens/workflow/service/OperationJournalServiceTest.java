package com.portfolens.workflow.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.portfolens.workflow.domain.WorkflowInstance;
import com.portfolens.workflow.dto.CreateWorkflowRequest;
import com.portfolens.workflow.exception.IdempotencyConflictException;
import com.portfolens.workflow.repository.WorkflowExecutionLogRepository;
import com.portfolens.workflow.repository.WorkflowInstanceRepository;
import com.portfolens.workflow.repository.WorkflowStepRepository;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OperationJournalServiceTest {
    @Mock WorkflowInstanceRepository workflows;
    @Mock WorkflowStepRepository steps;
    @Mock WorkflowExecutionLogRepository logs;
    OperationJournalService journal;
    private final ObjectMapper mapper = new ObjectMapper();

    @org.junit.jupiter.api.BeforeEach void setUp() { journal = new OperationJournalService(workflows, steps, logs, mapper); }

    @Test void sameIdempotencyKeyAndRequestReturnsExistingWorkflow() {
        var request = new CreateWorkflowRequest("TRADE", Map.of("symbol", "INFY"), List.of(new CreateWorkflowRequest.StepDefinition("reserve", 1, 2)));
        String canonical;
        try { canonical = mapper.writeValueAsString(Map.of("workflowType", "TRADE", "requestPayload", Map.of("symbol", "INFY"), "steps", List.of(new CreateWorkflowRequest.StepDefinition("reserve", 1, 2)))); }
        catch (Exception ex) { throw new IllegalStateException(ex); }
        var existing = new WorkflowInstance(UUID.randomUUID(), UUID.randomUUID(), "key-1", sha256(canonical), "TRADE", "{\"symbol\":\"INFY\"}");
        when(workflows.findByIdempotencyKey("key-1")).thenReturn(Optional.of(existing));

        assertEquals(existing, journal.createWorkflow("key-1", request));
        verify(workflows, never()).saveAndFlush(any());
    }

    @Test void sameIdempotencyKeyWithDifferentRequestConflicts() {
        var request = new CreateWorkflowRequest("TRADE", Map.of("symbol", "TCS"), List.of(new CreateWorkflowRequest.StepDefinition("reserve", 1, 2)));
        var existing = new WorkflowInstance(UUID.randomUUID(), UUID.randomUUID(), "key-1", "0".repeat(64), "TRADE", "{}");
        when(workflows.findByIdempotencyKey("key-1")).thenReturn(Optional.of(existing));

        assertThrows(IdempotencyConflictException.class, () -> journal.createWorkflow("key-1", request));
    }

    private String sha256(String value) {
        try { return java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(value.getBytes(java.nio.charset.StandardCharsets.UTF_8))); }
        catch (Exception ex) { throw new IllegalStateException(ex); }
    }
}
