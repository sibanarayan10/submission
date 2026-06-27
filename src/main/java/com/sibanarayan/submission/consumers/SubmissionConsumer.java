package com.sibanarayan.submission.consumers;

import com.sibanarayan.shared_package.enums.SubmissionStatus;
import com.sibanarayan.submission.entities.Submission;
import com.sibanarayan.submission.events.JudgeResultEvent;
import com.sibanarayan.submission.events.SubmissionEvent;
import com.sibanarayan.submission.httpClients.CoreServiceClient;
import com.sibanarayan.submission.models.response.TestCaseResponse;
import com.sibanarayan.submission.repositories.SubmissionRepositories;
import com.sibanarayan.submission.service.impl.JudgeServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class SubmissionConsumer {

    private final CoreServiceClient coreServiceClient;
    private final JudgeServiceImpl judgeService;
    private final SubmissionRepositories submissionRepository;
    private final KafkaTemplate<String, JudgeResultEvent> kafkaTemplate;
    private final SimpMessagingTemplate simpMessagingTemplate;

    private static final String RESULT_TOPIC = "judge.results";

    @KafkaListener(
            topics = "submission.pending",
            groupId = "submission-service-submission",
            containerFactory = "submissionEventFactory"
    )
    public void consume(SubmissionEvent event) {
        log.info("Received submission event for submissionId: {}",
                event.getSubmissionId());

        updateStatus(event.getSubmissionId(), SubmissionStatus.QUEUED);

        JudgeResultEvent result;


        try {
            List<TestCaseResponse> testCases =
                    coreServiceClient.getTestCases(event.getProblemId());

            Submission submission=Submission.builder()
                            .id(event.getSubmissionId())
                                    .status(SubmissionStatus.QUEUED)
                                            .problemId(event.getProblemId()).build();

            simpMessagingTemplate.convertAndSend("/topic/submission/" + event.getSubmissionId(),submission);


            if (testCases.isEmpty()) {
                log.warn("No test cases found for problem {}", event.getProblemId());
                result = JudgeResultEvent.builder()
                        .submissionId(event.getSubmissionId())
                        .status(SubmissionStatus.RUNTIME_ERROR)
                        .errorMessage("No test cases available for this problem")
                        .occurredAt(Instant.now())
                        .build();
            } else {
                updateStatus(event.getSubmissionId(), SubmissionStatus.RUNNING);
                result = judgeService.judge(event, testCases);
            }

        } catch (Exception e) {
            log.error("Unexpected error processing submission {}",
                    event.getSubmissionId(), e);
            result = JudgeResultEvent.builder()
                    .submissionId(event.getSubmissionId())
                    .status(SubmissionStatus.RUNTIME_ERROR)
                    .errorMessage("Internal error: " + e.getMessage())
                    .occurredAt(Instant.now())
                    .build();
        }


        Submission response=Submission.builder()
                                .problemId(event.getProblemId())
                                .createdAt(result.getOccurredAt())
                                .errorMessage(result.getErrorMessage())
                                .status(result.getStatus())
                                .passed(result.getPassed())
                                .total(result.getTotal())
                                .runtimeMs(result.getRuntimeMs())
                                .build();
        simpMessagingTemplate.convertAndSend("/topic/submission/" + event.getSubmissionId(),response);

        // publish result to judge.results
        kafkaTemplate.send(RESULT_TOPIC,
                event.getSubmissionId().toString(), result);

        log.info("Published result for submission {}: {}",
                event.getSubmissionId(), result.getStatus());
    }

    private void updateStatus(UUID submissionId, SubmissionStatus status) {
        submissionRepository.findById(submissionId).ifPresent(submission -> {
            submission.setStatus(status);
            submissionRepository.save(submission);
        });
    }

}