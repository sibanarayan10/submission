package com.sibanarayan.submission.consumers;

import com.sibanarayan.code.events.SubmissionResultEvent;
import com.sibanarayan.submission.events.JudgeResultEvent;
import com.sibanarayan.submission.repositories.SubmissionRepositories;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;


@Component
@RequiredArgsConstructor
@Slf4j
public class JudgeResultConsumer {
    private final SubmissionRepositories submissionRepository;
    private final KafkaTemplate<String, SubmissionResultEvent> kafkaTemplate;
    @KafkaListener(topics = "judge.results",
            groupId = "submission-service-result",
            containerFactory = "judgeResultFactory")
    public void consume(JudgeResultEvent event) {
        log.info("Received judge result for submission {}: {}",
                event.getSubmissionId(), event.getStatus());

        submissionRepository.findById(event.getSubmissionId())
                .ifPresentOrElse(submission -> {
                    submission.setStatus(event.getStatus());
                    submission.setErrorMessage(event.getErrorMessage());
                    submission.setTotal(event.getTotal());
                    submission.setPassed(event.getPassed());
                    submission.setRuntimeMs(event.getRuntimeMs());
                    submissionRepository.save(submission);
                    log.info("Submission {} updated with status {}",
                            event.getSubmissionId(), event.getStatus());

                    SubmissionResultEvent resultEvent=SubmissionResultEvent.builder()
                            .problemId(submission.getProblemId())
                            .userId(submission.getUserId())
                            .submissionId(submission.getId())
                            .status(event.getStatus())
                            .occurredAt(Instant.now())
                            .build();

                    publishEvent(resultEvent);

                }, () -> log.warn("Submission {} not found", event.getSubmissionId()));

    }


    private void publishEvent(SubmissionResultEvent event){
        kafkaTemplate.send("submission.result","",event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to Submission result event for submission {}",
                                 event.getSubmissionId(), ex);
                    } else {
                        log.info("Published Submission result event for submission {}",
                                 event.getSubmissionId());
                    }
                });
    }

}
