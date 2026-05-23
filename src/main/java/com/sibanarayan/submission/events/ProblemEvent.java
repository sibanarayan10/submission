package com.sibanarayan.submission.events;

import com.sibanarayan.submission.enums.EventType;
import com.sibanarayan.submission.enums.ProblemEventType;
import com.sibanarayan.submission.enums.ProgrammingLanguage;
import lombok.*;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProblemEvent {
    private UUID problemId;
    private String title;
    private EventType eventType;
    private Integer runtimeMs;
    private Integer memory;
    private Instant occurredAt;
    private Map<ProgrammingLanguage,String> ioByLanguage;
}