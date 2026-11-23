package com.portfolens.workflow.repository;

import com.portfolens.workflow.domain.WorkflowInstance;
import com.portfolens.workflow.domain.WorkflowStatus;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface WorkflowInstanceRepository extends JpaRepository<WorkflowInstance, UUID> {
    Optional<WorkflowInstance> findByWorkflowId(UUID workflowId);
    Optional<WorkflowInstance> findByIdempotencyKey(String idempotencyKey);
    Page<WorkflowInstance> findByStatus(WorkflowStatus status, Pageable pageable);
    Page<WorkflowInstance> findByWorkflowType(String workflowType, Pageable pageable);
    Page<WorkflowInstance> findByStatusAndWorkflowType(WorkflowStatus status, String workflowType, Pageable pageable);
    Page<WorkflowInstance> findByOwnerUserId(UUID ownerUserId, Pageable pageable);
    Page<WorkflowInstance> findByOwnerUserIdAndStatus(UUID ownerUserId, WorkflowStatus status, Pageable pageable);
    Page<WorkflowInstance> findByOwnerUserIdAndWorkflowType(UUID ownerUserId, String workflowType, Pageable pageable);
    Page<WorkflowInstance> findByOwnerUserIdAndStatusAndWorkflowType(UUID ownerUserId, WorkflowStatus status, String workflowType, Pageable pageable);
    Page<WorkflowInstance> findAll(Pageable pageable);
    List<WorkflowInstance> findByStatusIn(Collection<WorkflowStatus> statuses);
    @Query("select w from WorkflowInstance w where w.status in :statuses order by w.createdAt")
    List<WorkflowInstance> findIncomplete(@Param("statuses") Collection<WorkflowStatus> statuses);
    List<WorkflowInstance> findByStatusAndUpdatedAtBefore(WorkflowStatus status, Instant cutoff);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select w from WorkflowInstance w where w.workflowId = :workflowId")
    Optional<WorkflowInstance> findByWorkflowIdForUpdate(@Param("workflowId") UUID workflowId);
}
