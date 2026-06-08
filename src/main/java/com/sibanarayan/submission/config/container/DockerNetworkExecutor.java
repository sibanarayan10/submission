package com.sibanarayan.submission.config.container;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sibanarayan.submission.models.ExecutionResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

@Component
public class DockerNetworkExecutor {

    private static final Logger log = LoggerFactory.getLogger(DockerNetworkExecutor.class);
    private static final int WORKER_PORT = 8080;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Sends input to a worker container over HTTP and returns execution result.
     */
    public ExecutionResult executeViaNetwork(String containerId, String inputData)
            throws IOException, InterruptedException {

        String containerIp = getContainerIp(containerId);
        String url = "http://" + containerIp + ":" + WORKER_PORT + "/run";

        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();

        String requestBody = objectMapper.writeValueAsString(
                Map.of("stdin", inputData)
        );

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .timeout(Duration.ofSeconds(10)) // overall timeout
                .build();

        HttpResponse<String> response = client.send(
                request,
                HttpResponse.BodyHandlers.ofString()
        );

        return objectMapper.readValue(response.body(), ExecutionResult.class);
    }

    private String getContainerIp(String containerId) throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder(
                "docker", "inspect",
                "--format", "{{range .NetworkSettings.Networks}}{{.IPAddress}}{{end}}",
                containerId
        );
        Process process = pb.start();
        String ip = new String(process.getInputStream().readAllBytes()).trim();
        process.waitFor();
        return ip;
    }
}
