ALTER TABLE submission
DROP COLUMN "language";

ALTER TABLE submission
ADD COLUMN "programming_language" VARCHAR(30) NOT NULL DEFAULT 'PYTHON';