CREATE TABLE problem_snapshot(
    id              UUID            NOT NULL,
    problem_id      UUID            NOT NULL UNIQUE,
    record_status   VARCHAR(10)     NOT NULL DEFAULT 'ACTIVE',
    created_at      TIMESTAMP       NOT NULL DEFAULT now(),
    title           TEXT            NOT NULL,

    CONSTRAINT pk_problem_snapshot PRIMARY KEY(id),

     CONSTRAINT chk_record_status
           CHECK (record_status IN (
                   'ACTIVE',
                   'ARCHIVED',
                   'DELETED'
           ))
);