package com.portfolens.workflow;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.portfolens.workflow.dto.CreateWorkflowRequest;
import com.portfolens.workflow.service.OperationJournalService;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(properties = {
        "app.jwt.secret=AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=",
        "app.saga.recovery-interval=1h",
        "spring.jpa.hibernate.ddl-auto=validate"
})
class WorkflowJournalPostgresIntegrationTest {
    @Container static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource static void database(DynamicPropertyRegistry registry) {
        registry.add("WORKFLOW_DATABASE_URL", () -> POSTGRES.getJdbcUrl() + "?sslmode=disable");
        registry.add("DB_USER", POSTGRES::getUsername);
        registry.add("DB_PASSWORD", POSTGRES::getPassword);
        registry.add("spring.flyway.default-schema", () -> "workflow_it");
        registry.add("spring.flyway.schemas", () -> "workflow_it");
        registry.add("spring.jpa.properties.hibernate.default_schema", () -> "workflow_it");
    }

    @Autowired OperationJournalService journal;

    @Test void concurrentDuplicateRequestsCreateOneDurableWorkflowAndItsInitialJournal() throws Exception {
        String key = "pg-it-" + java.util.UUID.randomUUID();
        var request = new CreateWorkflowRequest("INTEGRATION_TEST", Map.of("value", 7), List.of(
                new CreateWorkflowRequest.StepDefinition("first", 1, 2)));
        var first = CompletableFuture.supplyAsync(() -> journal.createWorkflow(key, request));
        var second = CompletableFuture.supplyAsync(() -> journal.createWorkflow(key, request));

        var a = first.get(); var b = second.get();
        assertEquals(a.getWorkflowId(), b.getWorkflowId());
        assertEquals(2, journal.getLogs(a.getWorkflowId(), org.springframework.data.domain.PageRequest.of(0, 10)).getTotalElements());
        assertEquals(1, journal.getSteps(a.getWorkflowId()).size());
    }
}
