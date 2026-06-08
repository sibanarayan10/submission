package com.sibanarayan.submission.service.impl;

import com.sibanarayan.code.enums.ProgrammingLanguage;
import com.sibanarayan.code.enums.RecordStatus;
import com.sibanarayan.code.enums.SubmissionStatus;
import com.sibanarayan.submission.config.container.ContainerPoolManager;
import com.sibanarayan.submission.config.container.DockerExecutor;
import com.sibanarayan.submission.entities.Submission;
import com.sibanarayan.submission.events.JudgeResultEvent;
import com.sibanarayan.submission.events.SubmissionEvent;
import com.sibanarayan.submission.exceptions.PoolTimeoutException;
import com.sibanarayan.submission.models.ExecutionResult;
import com.sibanarayan.submission.models.response.TestCaseResponse;
import com.sibanarayan.submission.repositories.ProblemSnapshotRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class JudgeServiceImpl {

    private final DockerExecutor dockerExecutor;
    private final ContainerPoolManager poolManager;          // ← replaces direct DockerExecutor for lifecycle
    private final ProblemSnapshotRepository problemSnapshotRepository;
    private final SimpMessagingTemplate simpMessagingTemplate;

    public JudgeResultEvent judge(SubmissionEvent event, List<TestCaseResponse> testCases)
            throws IOException, InterruptedException {

        ProgrammingLanguage language = event.getLanguage();
        File codeFile = null;
        String containerId = null;

        try {
            // 1. Build full source: user solution + ioByLanguage harness
            StringBuilder source = new StringBuilder(event.getSolution());
            problemSnapshotRepository
                    .findByProblemIdAndRecordStatus(event.getProblemId(), RecordStatus.ACTIVE)
                    .ifPresent(p -> {
                        String harness = p.getIoByLanguage().get(language);
                        if (harness != null) source.append(harness);
                    });

            // 2. Write to temp file
            codeFile = createTempFile(source.toString(), language);

            // 3. Borrow a pre-warmed container (blocks until one is free or timeout)
            //    PoolTimeoutException is caught below and mapped to RUNTIME_ERROR
            containerId = poolManager.borrow(language);
            log.info("Borrowed container {} for submission {}", containerId, event.getSubmissionId());

            // 4. Copy code into container
            dockerExecutor.copyFile(containerId, codeFile, language);

            // 5. Compile (Java only)
            dockerExecutor.compile(containerId, language);

            // 6. Run test cases SEQUENTIALLY — fail fast on first failure
            for (int i = 0; i < testCases.size(); i++) {
                TestCaseResponse tc = testCases.get(i);

                log.info("Running test case {}/{} for submission {}",
                        i + 1, testCases.size(), event.getSubmissionId());

                ExecutionResult result = dockerExecutor.executeFile(containerId, language, tc.getInputData());

                // TLE
                if (result.isTle()) {
                    return buildResult(event.getSubmissionId(),
                            SubmissionStatus.TIME_LIMIT_EXCEEDED,
                            result.getRuntimeMs(), null,
                            i, testCases.size(),
                            "Time limit exceeded on test case " + (i + 1));
                }

                // Runtime error
                if (result.getExitCode() != 0) {
                    return buildResult(event.getSubmissionId(),
                            SubmissionStatus.RUNTIME_ERROR,
                            result.getRuntimeMs(), null,
                            i, testCases.size(),
                            result.getStderr());
                }

                // Wrong answer
                String actual   = normalize(result.getStdout().trim());
                String expected = normalize(tc.getExpectedOutput().trim());
                if (!actual.equals(expected)) {
                    return buildResult(event.getSubmissionId(),
                            SubmissionStatus.WRONG_ANSWER,
                            result.getRuntimeMs(), null,
                            i, testCases.size(),
                            "Test case " + (i + 1) + " failed. Expected: " + expected + ", Got: " + actual);
                }

                // Push live progress over WebSocket — no sleep needed
                pushProgress(event.getSubmissionId(), event.getProblemId(), i + 1, testCases.size());
            }

            return buildResult(event.getSubmissionId(),
                    SubmissionStatus.ACCEPTED,
                    0, null,          // runtimeMs: compute real value if needed
                    testCases.size(), testCases.size(),
                    null);

        } catch (PoolTimeoutException e) {
            log.warn("Pool timeout for submission {}: {}", event.getSubmissionId(), e.getMessage());
            return buildResult(event.getSubmissionId(),
                    SubmissionStatus.RUNTIME_ERROR,
                    null, null, 0, testCases.size(),
                    "Judge is busy, please resubmit shortly.");

        } catch (IOException e) {
            log.error("IO error for submission {}", event.getSubmissionId(), e);
            return buildResult(event.getSubmissionId(),
                    SubmissionStatus.RUNTIME_ERROR,
                    null, null, 0, testCases.size(),
                    "Internal error: " + e.getMessage());

        } finally {
            // Always clean up temp file
            if (codeFile != null) {
                try { Files.deleteIfExists(codeFile.toPath()); } catch (Exception ignored) {}
            }
            // Always return container to pool — cleanup of /app happens inside returnContainer
            if (containerId != null) {
                poolManager.returnContainer(language, containerId);
                log.info("Returned container {} for submission {}", containerId, event.getSubmissionId());
            }
        }
    }

    private void pushProgress(UUID submissionId, UUID problemId, int passed, int total) {
        Submission progress = Submission.builder()
                .problemId(problemId)
                .status(SubmissionStatus.RUNNING)
                .passed(passed)
                .total(total)
                .build();
        simpMessagingTemplate.convertAndSend("/topic/submission/" + submissionId, progress);
    }

    private File createTempFile(String source, ProgrammingLanguage language) throws IOException {
        String ext = switch (language) {
            case PYTHON -> ".py";
            case JAVA   -> ".java";
            default     -> ".txt";
        };
        File file = File.createTempFile("submission_", ext);
        try (FileWriter w = new FileWriter(file)) {
            w.write(source);
        }
        return file;
    }

    private String normalize(String output) {
        return output.replaceAll("[\\[\\],]", " ").trim().replaceAll("\\s+", " ");
    }

    private JudgeResultEvent buildResult(UUID submissionId, SubmissionStatus status,
                                         Integer runtimeMs, Integer memoryKb,
                                         Integer passed, Integer total, String errorMessage) {
        return JudgeResultEvent.builder()
                .submissionId(submissionId)
                .status(status)
                .runtimeMs(runtimeMs)
                .memoryKb(memoryKb)
                .passed(passed)
                .total(total)
                .errorMessage(errorMessage)
                .occurredAt(Instant.now())
                .build();
    }
}
