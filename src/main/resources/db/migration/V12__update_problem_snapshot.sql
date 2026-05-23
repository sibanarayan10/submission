ALTER TABLE problem_snapshot
DROP CONSTRAINT pk_problem_snapshot,

ADD COLUMN io_by_language JSONB NOT NULL DEFAULT '{}'::jsonb,

DROP COLUMN id,

ADD CONSTRAINT pk_problem_snapshot
PRIMARY KEY (problem_id);

CREATE INDEX idx_problem_snapshot_io_by_language
ON problem_snapshot
USING GIN (io_by_language);

