package com.sibanarayan.submission.events;


import com.sibanarayan.submission.enums.EventType;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Builder
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class UserEvent {
    private UUID id;
    private String email;
    private Instant occurredAt;
    private EventType eventType;
    private  String name;
}

