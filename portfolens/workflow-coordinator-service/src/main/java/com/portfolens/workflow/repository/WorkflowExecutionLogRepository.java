package com.portfolens.workflow.repository;

import com.portfolens.workflow.domain.WorkflowExecutionLog;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkflowExecutionLogRepository extends JpaRepository<WorkflowExecutionLog, UUID> {
    Page<WorkflowExecutionLog> findByWorkflow_WorkflowIdOrderByCreatedAtAscIdAsc(UUID workflowId, Pageable pageable);
    List<WorkflowExecutionLog> findByWorkflow_WorkflowIdOrderByCreatedAtAscIdAsc(UUID workflowId);
    List<WorkflowExecutionLog> findByStepIdOrderByCreatedAtAsc(UUID stepId);
}
