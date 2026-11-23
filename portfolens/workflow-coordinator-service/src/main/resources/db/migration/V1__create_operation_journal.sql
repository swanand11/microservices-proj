CREATE TABLE workflow_instances (
    id UUID PRIMARY KEY,
    workflow_id UUID NOT NULL UNIQUE,
    idempotency_key VARCHAR(255) NOT NULL UNIQUE,
    request_hash VARCHAR(64) NOT NULL,
    workflow_type VARCHAR(120) NOT NULL,
    status VARCHAR(32) NOT NULL,
    request_payload JSONB NOT NULL,
    result_payload JSONB,
    current_step VARCHAR(160),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    completed_at TIMESTAMPTZ,
    version BIGINT NOT NULL DEFAULT 0
);
CREATE INDEX idx_workflow_instances_status ON workflow_instances(status);
CREATE INDEX idx_workflow_instances_created_at ON workflow_instances(created_at);
CREATE INDEX idx_workflow_instances_status_updated_at ON workflow_instances(status, updated_at);

CREATE TABLE workflow_steps (
    id UUID PRIMARY KEY,
    step_id UUID NOT NULL UNIQUE,
    workflow_id UUID NOT NULL REFERENCES workflow_instances(workflow_id) ON DELETE RESTRICT,
    step_name VARCHAR(160) NOT NULL,
    step_order INTEGER NOT NULL,
    status VARCHAR(32) NOT NULL,
    retry_count INTEGER NOT NULL DEFAULT 0,
    max_retries INTEGER NOT NULL DEFAULT 3,
    compensation_status VARCHAR(32) NOT NULL,
    last_error TEXT,
    started_at TIMESTAMPTZ,
    completed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_workflow_steps_name UNIQUE(workflow_id, step_name),
    CONSTRAINT uq_workflow_steps_order UNIQUE(workflow_id, step_order)
);
CREATE INDEX idx_workflow_steps_workflow_order ON workflow_steps(workflow_id, step_order);
CREATE INDEX idx_workflow_steps_status ON workflow_steps(status);
CREATE INDEX idx_workflow_steps_created_at ON workflow_steps(created_at);

CREATE TABLE workflow_execution_log (
    id UUID PRIMARY KEY,
    workflow_id UUID NOT NULL REFERENCES workflow_instances(workflow_id) ON DELETE RESTRICT,
    step_id UUID,
    event_type VARCHAR(40) NOT NULL,
    previous_status VARCHAR(32),
    new_status VARCHAR(32),
    message TEXT NOT NULL,
    metadata JSONB,
    created_at TIMESTAMPTZ NOT NULL
);
CREATE INDEX idx_workflow_execution_log_workflow_created ON workflow_execution_log(workflow_id, created_at, id);
CREATE INDEX idx_workflow_execution_log_step ON workflow_execution_log(step_id);
CREATE INDEX idx_workflow_execution_log_created ON workflow_execution_log(created_at);

CREATE FUNCTION reject_workflow_log_mutation() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    RAISE EXCEPTION 'workflow_execution_log is append-only';
END;
$$;
CREATE TRIGGER workflow_execution_log_append_only
    BEFORE UPDATE OR DELETE ON workflow_execution_log
    FOR EACH ROW EXECUTE FUNCTION reject_workflow_log_mutation();
