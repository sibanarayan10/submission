CREATE TABLE user_snapshot(
    id                  UUID            NOT NULL,
    user_id             UUID            NOT NULL UNIQUE,
    record_status       VARCHAR(10)     NOT NULL DEFAULT 'ACTIVE',
    created_at          TIMESTAMP       NOT NULL DEFAULT now(),
    email               VARCHAR(20)     NOT NULL,
    user_name           VARCHAR(30)     NOT NULL,

    CONSTRAINT pk_user_snapshot PRIMARY KEY(id),

     CONSTRAINT chk_record_status
           CHECK (record_status IN (
                   'ACTIVE',
                   'ARCHIVED',
                   'DELETED'
           ))
);