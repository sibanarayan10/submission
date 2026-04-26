ALTER TABLE submission
DROP CONSTRAINT chk_submission_status,
ADD CONSTRAINT chk_submission_status    CHECK (status IN (
                                                       'PENDING',
                                                        'QUEUED',
                                                        'RUNNING',
                                                        'ACCEPTED',
                                                        'RUNTIME_ERROR',
                                                        'TIME_LIMIT_EXCEEDED',
                                                        'WRONG_ANSWER'
                                             ));

