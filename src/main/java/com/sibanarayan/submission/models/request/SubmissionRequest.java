package com.sibanarayan.submission.models.request;

import com.sibanarayan.submission.enums.ProgrammingLanguage;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class SubmissionRequest {
    private UUID id;

    @NotNull
    private UUID problemId;

    @NotBlank
    private UUID userId;

    @NotBlank
    private String solution;

    @NotNull
    private ProgrammingLanguage language;
}
