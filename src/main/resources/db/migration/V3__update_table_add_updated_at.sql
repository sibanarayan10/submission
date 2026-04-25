ALTER TABLE problem_snapshot
ADD COLUMN update_at TIMESTAMP;

ALTER TABLE user_snapshot
ADD COLUMN update_at TIMESTAMP;

ALTER TABLE submission
ADD COLUMN update_at TIMESTAMP;
