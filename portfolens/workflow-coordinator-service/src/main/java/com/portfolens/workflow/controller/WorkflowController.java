package com.portfolens.workflow.controller;

import com.portfolens.workflow.domain.WorkflowStatus;
import com.portfolens.workflow.dto.CreateWorkflowRequest;
import com.portfolens.workflow.dto.WorkflowDtos;
import com.portfolens.workflow.service.OperationJournalService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.core.Authentication;
import java.util.HashMap;

@RestController
@Validated
@RequestMapping("/api/v1/workflows")
public class WorkflowController {
    private final OperationJournalService journal;
    public WorkflowController(OperationJournalService journal) { this.journal = journal; }

    @PostMapping
    public ResponseEntity<WorkflowDtos.WorkflowView> create(
            @RequestHeader("Idempotency-Key") @NotBlank String idempotencyKey,
            @Valid @RequestBody CreateWorkflowRequest request, Authentication authentication) {
        var payload = new HashMap<>(request.requestPayload()); payload.put("ownerUserId", authentication.getName());
        var ownedRequest = new CreateWorkflowRequest(request.workflowType(), payload, request.steps());
        var workflow = journal.createWorkflow(idempotencyKey, ownedRequest, UUID.fromString(authentication.getName()));
        if (!UUID.fromString(authentication.getName()).equals(workflow.getOwnerUserId()))
            throw new org.springframework.security.access.AccessDeniedException("Workflow does not belong to the authenticated user");
        return ResponseEntity.ok()
                .body(journal.getWorkflow(workflow.getWorkflowId()));
    }
    @GetMapping("/{workflowId}")
    public WorkflowDtos.WorkflowView get(@PathVariable UUID workflowId, Authentication authentication) { return ownedWorkflow(workflowId, authentication); }
    @GetMapping("/{workflowId}/steps")
    public List<WorkflowDtos.StepView> steps(@PathVariable UUID workflowId, Authentication authentication) { ownedWorkflow(workflowId, authentication); return journal.getSteps(workflowId); }
    @GetMapping("/{workflowId}/logs")
    public Page<WorkflowDtos.LogView> logs(@PathVariable UUID workflowId,
            @RequestParam(defaultValue = "0") @Min(0) int page, @RequestParam(defaultValue = "50") @Min(1) int size, Authentication authentication) {
        ownedWorkflow(workflowId, authentication);
        return journal.getLogs(workflowId, page(page, size, Sort.by("createdAt").ascending()));
    }
    @GetMapping
    public Page<WorkflowDtos.WorkflowView> list(@RequestParam(required = false) WorkflowStatus status,
            @RequestParam(required = false) String workflowType, @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) int size, @RequestParam(defaultValue = "createdAt,desc") String sort, Authentication authentication) {
        String[] parts = sort.split(",", 2);
        if (!java.util.Set.of("createdAt", "updatedAt", "status", "workflowType").contains(parts[0]))
            throw new IllegalArgumentException("Unsupported sort field");
        Sort.Direction direction = parts.length == 2 ? Sort.Direction.fromString(parts[1]) : Sort.Direction.DESC;
        return journal.list(UUID.fromString(authentication.getName()), status, workflowType, page(page, size, Sort.by(direction, parts[0])));
    }
    private Pageable page(int page, int size, Sort sort) {
        if (page < 0 || size < 1 || size > 100) throw new IllegalArgumentException("page must be nonnegative and size must be between 1 and 100");
        return PageRequest.of(page, size, sort);
    }
    private WorkflowDtos.WorkflowView ownedWorkflow(UUID id, Authentication authentication) {
        if (!UUID.fromString(authentication.getName()).equals(journal.getInstance(id).getOwnerUserId()))
            throw new org.springframework.security.access.AccessDeniedException("Workflow does not belong to the authenticated user");
        return journal.getWorkflow(id);
    }
}
