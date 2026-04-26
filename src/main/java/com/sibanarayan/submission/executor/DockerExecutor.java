package com.sibanarayan.submission.executor;

import com.sibanarayan.submission.models.ExecutionResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.*;
import java.util.concurrent.TimeUnit;

@Component
@Slf4j
public class DockerExecutor {

    private static final int TIMEOUT_SECONDS = 20;
    private static final String PYTHON_IMAGE = "python:3.11-slim";

    public ExecutionResult execute(File codeFile, String input) {
        long startTime = System.currentTimeMillis();

        try {
            ProcessBuilder pb = new ProcessBuilder(
                    "docker", "run", "--rm",
                    "--memory=256m",
                    "--cpus=0.5",
                    "--network=none",
                    "-i",
                    "-v", codeFile.getAbsolutePath() + ":/app/solution.py:ro",
                    PYTHON_IMAGE,
                    "python3", "/app/solution.py"
            );
            pb.redirectErrorStream(false);

            Process process = pb.start();

            Thread inputThread = new Thread(() -> {
                try (BufferedWriter writer = new BufferedWriter(
                        new OutputStreamWriter(process.getOutputStream()))) {
                    writer.write(input);
                    writer.newLine();
                    writer.flush();
                } catch (IOException e) {
                    log.debug("Input thread closed: {}", e.getMessage());
                }
            });
            inputThread.start();

            StringBuilder stdout = new StringBuilder();
            StringBuilder stderr = new StringBuilder();

            Thread stdoutThread = new Thread(() -> {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        stdout.append(line).append("\n");
                    }
                } catch (IOException e) {
                    log.warn("Failed to read stdout", e);
                }
            });

            Thread stderrThread = new Thread(() -> {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(process.getErrorStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        stderr.append(line).append("\n");
                    }
                } catch (IOException e) {
                    log.warn("Failed to read stderr", e);
                }
            });

            stdoutThread.start();
            stderrThread.start();

            boolean finished = process.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS);

            if (!finished) {
                process.destroyForcibly();
                return ExecutionResult.builder()
                        .tle(true)
                        .stdout("")
                        .stderr("Time limit exceeded")
                        .exitCode(-1)
                        .runtimeMs(TIMEOUT_SECONDS * 1000)
                        .build();
            }

            stdoutThread.join(1000);
            stderrThread.join(1000);

            int runtimeMs = (int) (System.currentTimeMillis() - startTime);

            log.info("stdout: '{}'", stdout.toString().trim());
            log.info("stderr: '{}'", stderr.toString().trim());
            log.info("exitCode: {}", process.exitValue());

            return ExecutionResult.builder()
                    .tle(false)
                    .stdout(stdout.toString().trim())
                    .stderr(stderr.toString().trim())
                    .exitCode(process.exitValue())
                    .runtimeMs(runtimeMs)
                    .build();

        } catch (Exception e) {
            log.error("Docker execution failed", e);
            return ExecutionResult.builder()
                    .tle(false)
                    .stdout("")
                    .stderr(e.getMessage())
                    .exitCode(-1)
                    .runtimeMs(0)
                    .build();
        }
    }
}