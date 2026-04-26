package com.sibanarayan.submission.models;

import lombok.*;

@Getter  @Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExecutionResult {
    private String stdout;
    private String stderr;
    private int exitCode;
    private int runtimeMs;
    private boolean tle;
}
