ALTER TABLE problem_snapshot
ADD COLUMN runtime_ms INTEGER,
ADD COLUMN memory INTEGER;

ALTER TABLE submission
DROP COLUMN runtime_ms,
DROP COLUMN memory;