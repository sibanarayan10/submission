package com.sibanarayan.submission.httpClients;

import com.sibanarayan.submission.models.response.TestCaseResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

@Component
@Slf4j
public class CoreServiceClient {

    @Value("${core_service.url}")
    private String url;

    private  RestTemplate restTemplate;

    public CoreServiceClient(RestTemplateBuilder builder) {
        this.restTemplate = builder
                .connectTimeout(Duration.ofSeconds(3))
                .readTimeout(Duration.ofSeconds(5))
                .build();
    }


    public List<TestCaseResponse> getTestCases(UUID problemId) {
        try {
            ResponseEntity<List<TestCaseResponse>> response = restTemplate.exchange(
                    url + "/api/v1/problems/" + problemId + "/testCases/all",
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<>() {}
            );
            return response.getBody() != null ? response.getBody() : List.of();
        } catch (Exception e) {
            log.error("Failed to fetch test cases for problem {}", problemId, e);
            throw new RuntimeException("Could not fetch test cases", e);
        }
    }

}
