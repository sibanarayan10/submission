package com.sibanarayan.submission.models.response;

import com.sibanarayan.submission.models.ExecutionResult;

public record TestCaseResult(
        int index,
        TestCaseResponse testCase,
        ExecutionResult executionResult) {}