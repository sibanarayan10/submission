CREATE TABLE submission (
    id              UUID        NOT NULL,
    problem_id      UUID        NOT NULL ,
    user_id          UUID        NOT NULL,
    solution        TEXT        NOT NULL,
    status          VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    created_at      TIMESTAMP        NOT NULL DEFAULT now(),
    record_status   VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',

    CONSTRAINT submission_id PRIMARY KEY(id),
    CONSTRAINT submission_problem_id FOREIGN KEY(problem_id)
    REFERENCES problem_snapshot(problem_id),
    CONSTRAINT submission_user_id FOREIGN KEY(user_id)
    REFERENCES user_snapshot(user_id),

    CONSTRAINT chk_submission_status
        CHECK (status IN (
                'PENDING',
                'SOLVED'
        )),

  CONSTRAINT chk_record_status
        CHECK (record_status IN (
                'ACTIVE',
                'ARCHIVED',
                'DELETED'
        ))
);