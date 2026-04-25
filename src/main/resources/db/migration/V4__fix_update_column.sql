ALTER TABLE problem_snapshot DROP COLUMN update_at;
ALTER TABLE user_snapshot DROP COLUMN update_at;
ALTER TABLE submission DROP COLUMN update_at;


ALTER TABLE problem_snapshot
ADD COLUMN updated_at TIMESTAMP ;

ALTER TABLE user_snapshot
ADD COLUMN updated_at TIMESTAMP;

ALTER TABLE submission
ADD COLUMN updated_at TIMESTAMP;