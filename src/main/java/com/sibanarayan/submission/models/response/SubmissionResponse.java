package com.sibanarayan.submission.models.response;

import com.sibanarayan.code.enums.SubmissionStatus;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Builder
@Getter
@Setter
public class SubmissionResponse {
    private UUID id;
    private UUID userId;
    private UUID problemId;
    private SubmissionStatus status;
    private Instant createdAt;
    private Integer passed;
    private Integer total;
    private String errorMessage;
    private Integer runtimeMs;
}
