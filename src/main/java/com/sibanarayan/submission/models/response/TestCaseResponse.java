package com.sibanarayan.submission.models.response;

import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter @Setter
public class TestCaseResponse {
    private UUID id;
    private String inputData;
    private String expectedOutput;
    private boolean sample;
    private Integer sequenceOrder;
}
