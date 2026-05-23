package com.sibanarayan.submission.service.impl;

import com.sibanarayan.submission.enums.ProgrammingLanguage;
import com.sibanarayan.submission.enums.RecordStatus;
import com.sibanarayan.submission.enums.SubmissionStatus;
import com.sibanarayan.submission.events.JudgeResultEvent;
import com.sibanarayan.submission.events.SubmissionEvent;
import com.sibanarayan.submission.executor.DockerExecutor;
import com.sibanarayan.submission.models.ExecutionResult;
import com.sibanarayan.submission.models.response.TestCaseResponse;
import com.sibanarayan.submission.repositories.ProblemSnapshotRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
    private final ProblemSnapshotRepository problemSnapshotRepository;

    public JudgeResultEvent judge(SubmissionEvent event, List<TestCaseResponse> testCases) throws IOException,InterruptedException {
        File codeFile = null;
        int i=0;

        try {
            StringBuilder builder=new StringBuilder(event.getSolution());

            problemSnapshotRepository.findByProblemIdAndRecordStatus(event.getProblemId(), RecordStatus.ACTIVE).ifPresent((p)->{
                String mainMethodCode=p.getIoByLanguage().get(event.getLanguage());
                builder.append(mainMethodCode);
            });

            codeFile = createTempFile(builder.toString(), event.getLanguage());

            String containerId= dockerExecutor.startContainer(event.getLanguage());
            dockerExecutor.copyFile(containerId,codeFile,event.getLanguage());


            for (i = 0; i < testCases.size(); i++) {
                TestCaseResponse tc = testCases.get(i);
                log.info("Running test case {}/{} for submission {}",
                        i + 1, testCases.size(), event.getSubmissionId());

                ExecutionResult result =dockerExecutor.executeFile(containerId,event.getLanguage(),tc.getInputData());

                if (result.isTle()) {
                    dockerExecutor.destroy(containerId);
                    return buildResult(event.getSubmissionId(),
                            SubmissionStatus.TIME_LIMIT_EXCEEDED,
                            result.getRuntimeMs(), null,
                            i,
                            testCases.size(),
                            "Time limit exceeded on test case " + (i + 1));
                }

                if (result.getExitCode() != 0) {
                    dockerExecutor.destroy(containerId);
                    return buildResult(event.getSubmissionId(),
                            SubmissionStatus.RUNTIME_ERROR,
                            result.getRuntimeMs(), null,
                            i,
                            testCases.size(),
                            result.getStderr());
                }

                String actual = normalize(result.getStdout().trim());
                String expected =normalize(tc.getExpectedOutput().trim());

                if (!actual.equals(expected)) {
                    dockerExecutor.destroy(containerId);
                    return buildResult(event.getSubmissionId(),
                            SubmissionStatus.WRONG_ANSWER,
                            result.getRuntimeMs(), null,
                            i,
                            testCases.size(),
                            "Test case " + (i + 1) + " failed. " +
                                    "Expected: " + expected + ", Got: " + actual);
                }
            }

            dockerExecutor.destroy(containerId);

            return buildResult(event.getSubmissionId(),
                    SubmissionStatus.ACCEPTED,
                    500, null, testCases.size(),
                    testCases.size(), null);

        } catch (IOException e) {
            log.error("Failed to create temp file for submission {}",
                    event.getSubmissionId(), e);
            return buildResult(event.getSubmissionId(),
                    SubmissionStatus.RUNTIME_ERROR,
                    null, null,i, testCases.size(), "Internal error: " + e.getMessage());
        }catch (Exception e){
            log.error("Something went wrong");
            return buildResult(event.getSubmissionId(),
                    SubmissionStatus.RUNTIME_ERROR,
                    null, null,i, testCases.size(), "Internal error: " + e.getMessage());
        }finally {
            if (codeFile != null && codeFile.exists()) {
                Files.deleteIfExists(codeFile.toPath());
            }
        }
    }

    private File createTempFile(String sourceCode, ProgrammingLanguage language)
            throws IOException {


        String extension = switch (language) {
            case PYTHON -> ".py";
            case JAVA -> ".java";
            default -> ".txt";
        };

        File file = File.createTempFile("submission_", extension);
        try (FileWriter writer = new FileWriter(file)) {
            writer.write(sourceCode);
        }
        return file;
    }


    private String normalize(String output) {
        return output
                .replaceAll("[\\[\\],]", " ")
                .trim()
                .replaceAll("\\s+", " ");
    }
    private JudgeResultEvent buildResult(UUID submissionId,
                                         SubmissionStatus status,
                                         Integer runtimeMs,
                                         Integer memoryKb,
                                         Integer passed,
                                         Integer total,
                                         String errorMessage) {
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
