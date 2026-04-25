package com.sibanarayan.submission.events;

import com.sibanarayan.submission.enums.ProgrammingLanguage;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@AllArgsConstructor
@Builder
@Getter
@Setter
public class SubmissionEvent {
   private UUID problemId;
   private String solution;
   private ProgrammingLanguage language;

}
