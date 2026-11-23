package com.portfolens.workflow.controller;

import com.portfolens.workflow.dto.StockTransactionSagaRequest;
import com.portfolens.workflow.dto.WorkflowDtos;
import com.portfolens.workflow.service.OperationJournalService;
import com.portfolens.workflow.service.SagaOrchestratorService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/sagas")
public class SagaController {
    private final SagaOrchestratorService orchestrator;
    private final OperationJournalService journal;
    public SagaController(SagaOrchestratorService orchestrator, OperationJournalService journal) { this.orchestrator = orchestrator; this.journal = journal; }

    @PostMapping("/stock-transactions")
    public WorkflowDtos.WorkflowView start(@RequestHeader("Idempotency-Key") @NotBlank @Size(max = 100) String key,
            @RequestHeader("Authorization") String authorization, @Valid @RequestBody StockTransactionSagaRequest request,
            Authentication authentication) {
        return orchestrator.startStockTransaction(request, key, authorization, authentication.getName());
    }
    @GetMapping("/{workflowId}")
    public WorkflowDtos.WorkflowView get(@PathVariable UUID workflowId, Authentication authentication) {
        WorkflowDtos.WorkflowView view = journal.getWorkflow(workflowId); orchestrator.assertOwner(view.requestPayload(), authentication.getName()); return view;
    }
    @GetMapping("/{workflowId}/steps")
    public List<WorkflowDtos.StepView> steps(@PathVariable UUID workflowId, Authentication authentication) {
        WorkflowDtos.WorkflowView view = journal.getWorkflow(workflowId); orchestrator.assertOwner(view.requestPayload(), authentication.getName()); return view.steps();
    }
    @PostMapping("/{workflowId}/recover")
    public WorkflowDtos.WorkflowView recover(@PathVariable UUID workflowId, @RequestHeader("Authorization") String authorization, Authentication authentication) {
        return orchestrator.recover(workflowId, authorization, authentication.getName());
    }
    @PostMapping("/{workflowId}/retry")
    public WorkflowDtos.WorkflowView retry(@PathVariable UUID workflowId, @RequestHeader("Authorization") String authorization, Authentication authentication) {
        return orchestrator.recover(workflowId, authorization, authentication.getName());
    }
}
