package com.portfolens.workflow.service;

import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class WorkflowRecoveryService {
    private static final Logger log = LoggerFactory.getLogger(WorkflowRecoveryService.class);
    private final SagaOrchestratorService orchestrator;
    public WorkflowRecoveryService(SagaOrchestratorService orchestrator) { this.orchestrator = orchestrator; }
    @Scheduled(fixedDelayString = "${app.saga.recovery-interval:5000ms}")
    public void recoverIncomplete() {
        log.debug("Scanning durable journal for workflows needing controlled recovery at {}", Instant.now());
        try { orchestrator.recoverIncompleteWithoutCredentials(); }
        catch (RuntimeException failure) { log.warn("Could not scan the workflow journal for recovery", failure); }
    }
}
