package com.sibanarayan.submission.events;

import com.sibanarayan.submission.enums.ProblemEventType;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProblemEvent {
    private UUID problemId;
    private String title;
    private ProblemEventType eventType;
    private Instant occurredAt;
}