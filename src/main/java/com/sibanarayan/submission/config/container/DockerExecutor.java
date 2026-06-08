package com.sibanarayan.submission.config.container;

import com.sibanarayan.code.enums.ProgrammingLanguage;
import com.sibanarayan.submission.models.ExecutionResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Component
@Slf4j
public class DockerExecutor {

    private static final int TIMEOUT_SECONDS = 20;

    public String startContainer(ProgrammingLanguage language) throws IOException, InterruptedException {
        return startContainer(language, 256, 0.5);
    }

    public String startContainer(ProgrammingLanguage language, Integer memoryMb, Double cpus)
            throws IOException, InterruptedException {
        if (language == null) {
            throw new IllegalArgumentException("Language must not be null");
        }

        String image = getImage(language);

        ProcessBuilder pb = new ProcessBuilder(
                "docker", "run", "-d",
                "--memory=" + memoryMb + "m",
                "--cpus=" + cpus,
                "--network=none",
                image,
                "sleep",
                "infinity"
        );
        pb.redirectErrorStream(false);
        Process process = pb.start();

        boolean finished = process.waitFor(110, TimeUnit.SECONDS);
        if (!finished || process.exitValue() != 0) {
            String err = new String(process.getErrorStream().readAllBytes());
            throw new IOException("Failed to start container: " + err);
        }

        String containerId = new String(process.getInputStream().readAllBytes()).trim();
        log.info("Started container: {}", containerId);
        createAppDirectory(containerId);
        return containerId;
    }

    public void copyFile(String containerId, File codeFile, ProgrammingLanguage language)
            throws IOException, InterruptedException {
        String extension = getExtension(language);

        ProcessBuilder pb = new ProcessBuilder(
                "docker", "cp",
                codeFile.getAbsolutePath(),
                containerId + ":/app/Submission" + extension
        );
        pb.redirectErrorStream(true);
        Process process = pb.start();

        boolean finished = process.waitFor(10, TimeUnit.SECONDS);
        if (!finished || process.exitValue() != 0) {
            String err = new String(process.getInputStream().readAllBytes());
            throw new IOException("Failed to copy file to container: " + err);
        }
        log.info("Copied code file to container {}", containerId);
    }
    public void compile(String containerId, ProgrammingLanguage language)
            throws IOException, InterruptedException {

        if (language != ProgrammingLanguage.JAVA) return; // Python needs no compilation

        ProcessBuilder pb = new ProcessBuilder(
                "docker", "exec", containerId,
                "sh", "-c", "javac /app/Submission.java"
        );
        pb.redirectErrorStream(true);
        Process process = pb.start();

        boolean finished = process.waitFor(30, TimeUnit.SECONDS);
        String output = new String(process.getInputStream().readAllBytes());

        if (!finished || process.exitValue() != 0) {
            throw new IOException("Compilation failed: " + output);
        }
        log.info("Compilation successful for container {}", containerId);
    }
    public ExecutionResult executeFile(String containerId, ProgrammingLanguage language, String input) {
        long startTime = System.currentTimeMillis();

        try {
            List<String> runCommand = getRunCommand(language);

            List<String> cmd = new ArrayList<>();
            cmd.addAll(List.of("docker", "exec", "-i", containerId));
            cmd.addAll(runCommand);

            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.redirectErrorStream(false);

            Process process = pb.start();

            String parsedInput=parseInput(input);

            Thread inputThread = new Thread(() -> {
                try (BufferedWriter writer = new BufferedWriter(
                        new OutputStreamWriter(process.getOutputStream()))) {
                    writer.write(parsedInput);
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
                log.warn("TLE in container {}", containerId);
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
            log.error("Exec failed in container {}", containerId, e);
            return ExecutionResult.builder()
                    .tle(false)
                    .stdout("")
                    .stderr(e.getMessage())
                    .exitCode(-1)
                    .runtimeMs(0)
                    .build();
        }
    }

    public void cleanWorkDir(String containerId) throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder(
                "docker", "exec", containerId,
                "sh", "-c", "rm -f /app/Submission.java /app/Submission.class /app/Submission.py"
        );
        pb.redirectErrorStream(true);
        Process process = pb.start();
        boolean finished = process.waitFor(5, TimeUnit.SECONDS);
        if (!finished || process.exitValue() != 0) {
            String err = new String(process.getInputStream().readAllBytes());
            throw new IOException("Failed to clean container workspace: " + err);
        }
        log.debug("Cleaned workspace in container {}", containerId);
    }
    public void destroy(String containerId) {
        try {
            ProcessBuilder pb = new ProcessBuilder(
                    "docker", "rm", "-f", containerId
            );
            pb.redirectErrorStream(true);
            Process process = pb.start();

            boolean finished = process.waitFor(10, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
            }
            log.info("Removed container: {}", containerId);
        } catch (Exception e) {
            log.error("Failed to remove container {}, it may be leaking", containerId, e);
        }
    }

    private void createAppDirectory(String containerId)
            throws IOException, InterruptedException {

        ProcessBuilder pb = new ProcessBuilder(
                "docker", "exec", containerId,
                "mkdir", "-p", "/app"
        );
        pb.redirectErrorStream(true);
        Process process = pb.start();

        boolean finished = process.waitFor(10, TimeUnit.SECONDS);
        if (!finished || process.exitValue() != 0) {
            String err = new String(process.getInputStream().readAllBytes());
            throw new IOException("Failed to create /app directory: " + err);
        }

        log.info("Created /app directory in container {}", containerId);
    }

    private String getImage(ProgrammingLanguage language) {
        return switch (language) {
            case PYTHON -> "python:3.11-slim";
            case JAVA -> "eclipse-temurin:21-jdk";
            default -> ".txt";
        };
    }

    private String getExtension(ProgrammingLanguage language) {
        return switch (language) {
            case PYTHON -> ".py";
            case JAVA -> ".java";
            default -> ".txt";
        };
    }

    private List<String> getRunCommand(ProgrammingLanguage language) {
        return switch (language) {
            case PYTHON -> List.of("python3", "/app/Submission.py");
            case JAVA -> List.of("java", "-cp", "/app", "Submission"); // no javac here
            default -> new ArrayList<>();
        };
    }

    private String parseInput(String input) {

        return Arrays.stream(input.split("\n"))
                .map(line -> line.split("=", 2)[1])
                .map(value -> value.replaceAll("[\\[\\],]", " "))
                .map(String::trim)
                .collect(Collectors.joining("\n"));
    }
}