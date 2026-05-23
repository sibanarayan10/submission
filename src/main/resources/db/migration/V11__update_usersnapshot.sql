ALTER TABLE user_snapshot

DROP CONSTRAINT pk_user_snapshot,

DROP COLUMN id,
DROP COLUMN updated_at,
DROP COLUMN user_name,
DROP COLUMN user_id,

ADD COLUMN name VARCHAR(255),
ADD COLUMN user_id UUID NOT NULL,

ADD CONSTRAINT pk_user_snapshot_user_id PRIMARY KEY(user_id);