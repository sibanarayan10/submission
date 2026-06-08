package com.sibanarayan.submission.events;

import com.sibanarayan.code.enums.SubmissionStatus;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JudgeResultEvent {
    private UUID submissionId;
    private SubmissionStatus status;
    private Integer runtimeMs;
    private Integer memoryKb;
    private Integer passed;
    private Integer total;
    private String errorMessage;
    private Instant occurredAt;
}
