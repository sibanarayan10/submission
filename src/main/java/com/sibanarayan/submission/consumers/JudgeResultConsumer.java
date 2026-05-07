package com.sibanarayan.submission.consumers;

import com.sibanarayan.submission.events.JudgeResultEvent;
import com.sibanarayan.submission.repositories.SubmissionRepositories;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;


@Component
@RequiredArgsConstructor
@Slf4j
public class JudgeResultConsumer {
    private final SubmissionRepositories submissionRepository;
    @KafkaListener(topics = "judge.results",
            groupId = "submission-service-result",
            containerFactory = "judgeResultFactory")
    public void consume(JudgeResultEvent event) {
        log.info("Received judge result for submission {}: {}",
                event.getSubmissionId(), event.getStatus());

        submissionRepository.findById(event.getSubmissionId())
                .ifPresentOrElse(submission -> {
                    submission.setStatus(event.getStatus());
                    submission.setRuntimeMs(event.getRuntimeMs());
                    submission.setMemoryKb(event.getMemoryKb());
                    submission.setErrorMessage(event.getErrorMessage());
                    submissionRepository.save(submission);
                    log.info("Submission {} updated with status {}",
                            event.getSubmissionId(), event.getStatus());
                }, () -> log.warn("Submission {} not found", event.getSubmissionId()));
    }

}
