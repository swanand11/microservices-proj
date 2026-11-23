package com.portfolens.workflow.repository;

import com.portfolens.workflow.domain.StepStatus;
import com.portfolens.workflow.domain.WorkflowStep;
import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.time.Instant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface WorkflowStepRepository extends JpaRepository<WorkflowStep, UUID> {
    List<WorkflowStep> findByWorkflow_WorkflowIdOrderByStepOrder(UUID workflowId);
    Optional<WorkflowStep> findByWorkflow_WorkflowIdAndStepName(UUID workflowId, String stepName);
    List<WorkflowStep> findByStatusIn(Collection<StepStatus> statuses);
    List<WorkflowStep> findByStatus(StepStatus status);
    List<WorkflowStep> findByStatusAndNextRetryAtBefore(StepStatus status, Instant cutoff);
    List<WorkflowStep> findByWorkflow_WorkflowIdAndStatusOrderByStepOrder(UUID workflowId, StepStatus status);
    @Lock(LockModeType.OPTIMISTIC_FORCE_INCREMENT)
    @Query("select s from WorkflowStep s where s.workflow.workflowId = :workflowId and s.stepName = :stepName")
    Optional<WorkflowStep> findForUpdate(@Param("workflowId") UUID workflowId, @Param("stepName") String stepName);
}
