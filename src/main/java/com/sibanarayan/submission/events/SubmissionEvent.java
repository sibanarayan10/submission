package com.sibanarayan.submission.events;

import com.sibanarayan.code.enums.ProgrammingLanguage;
import lombok.*;

import java.util.UUID;

@AllArgsConstructor
@Builder
@Getter
@Setter
@NoArgsConstructor
public class SubmissionEvent {
   private UUID submissionId;
   private UUID problemId;
   private String solution;
   private ProgrammingLanguage language;

}
