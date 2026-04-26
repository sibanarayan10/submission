package com.sibanarayan.submission.service.impl;

import com.sibanarayan.submission.enums.ProgrammingLanguage;
import com.sibanarayan.submission.enums.SubmissionStatus;
import com.sibanarayan.submission.events.JudgeResultEvent;
import com.sibanarayan.submission.events.SubmissionEvent;
import com.sibanarayan.submission.executor.DockerExecutor;
import com.sibanarayan.submission.models.ExecutionResult;
import com.sibanarayan.submission.models.response.TestCaseResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class JudgeServiceImpl {

    private final DockerExecutor dockerExecutor;

    public JudgeResultEvent judge(SubmissionEvent event, List<TestCaseResponse> testCases) {
        File codeFile = null;

        try {
            codeFile = createTempFile(event.getSolution(), event.getLanguage());

            for (int i = 0; i < testCases.size(); i++) {
                TestCaseResponse tc = testCases.get(i);
                log.info("Running test case {}/{} for submission {}",
                        i + 1, testCases.size(), event.getSubmissionId());

                ExecutionResult result = dockerExecutor.execute(codeFile, tc.getInputData());

                if (result.isTle()) {
                    return buildResult(event.getSubmissionId(),
                            SubmissionStatus.TIME_LIMIT_EXCEEDED,
                            result.getRuntimeMs(), null,
                            "Time limit exceeded on test case " + (i + 1));
                }

                if (result.getExitCode() != 0) {
                    return buildResult(event.getSubmissionId(),
                            SubmissionStatus.RUNTIME_ERROR,
                            result.getRuntimeMs(), null,
                            result.getStderr());
                }

                String actual = result.getStdout().trim();
                String expected = tc.getExpectedOutput().trim();

                if (!actual.equals(expected)) {
                    return buildResult(event.getSubmissionId(),
                            SubmissionStatus.WRONG_ANSWER,
                            result.getRuntimeMs(), null,
                            "Test case " + (i + 1) + " failed. " +
                                    "Expected: " + expected + ", Got: " + actual);
                }
            }

            // all test cases passed
            TestCaseResponse lastTc = testCases.get(testCases.size() - 1);
            ExecutionResult lastResult = dockerExecutor.execute(
                    codeFile, lastTc.getInputData());

            return buildResult(event.getSubmissionId(),
                    SubmissionStatus.ACCEPTED,
                    lastResult.getRuntimeMs(), null, null);

        } catch (IOException e) {
            log.error("Failed to create temp file for submission {}",
                    event.getSubmissionId(), e);
            return buildResult(event.getSubmissionId(),
                    SubmissionStatus.RUNTIME_ERROR,
                    null, null, "Internal error: " + e.getMessage());
        } finally {
            if (codeFile != null && codeFile.exists()) {
                codeFile.delete();
            }
        }
    }

    private File createTempFile(String sourceCode, ProgrammingLanguage language)
            throws IOException {
        String extension = switch (language) {
            case PYTHON -> ".py";
            case JAVA -> ".java";
            case JAVASCRIPT -> ".js";
            default -> ".txt";
        };

        File file = File.createTempFile("submission_", extension);
        try (FileWriter writer = new FileWriter(file)) {
            writer.write(sourceCode);
        }
        return file;
    }

    private JudgeResultEvent buildResult(UUID submissionId,
                                         SubmissionStatus status,
                                         Integer runtimeMs,
                                         Integer memoryKb,
                                         String errorMessage) {
        return JudgeResultEvent.builder()
                .submissionId(submissionId)
                .status(status)
                .runtimeMs(runtimeMs)
                .memoryKb(memoryKb)
                .errorMessage(errorMessage)
                .occurredAt(Instant.now())
                .build();
    }
}
