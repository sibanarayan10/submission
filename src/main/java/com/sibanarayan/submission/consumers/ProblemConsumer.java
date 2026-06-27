package com.sibanarayan.submission.consumers;


import com.sibanarayan.shared_package.enums.RecordStatus;
import com.sibanarayan.shared_package.events.ProblemEvent;
import com.sibanarayan.submission.entities.ProblemSnapshot;
import com.sibanarayan.submission.repositories.ProblemSnapshotRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;


@Slf4j
@AllArgsConstructor
@Component
public class ProblemConsumer {

    private final ProblemSnapshotRepository problemSnapshotRepository;
    @KafkaListener(topics="problem.events",
                    groupId="submission-service-problem",
                    containerFactory = "problemEventFactory")
    public void consume(ProblemEvent event){
        switch (event.getEventType()){
            case CREATE -> handleProblemCreation(event);
            case DELETE -> handleProblemDeletion(event);

        }
    }

    private void handleProblemCreation(ProblemEvent event){
        if (problemSnapshotRepository.existsById(event.getProblemId())) {
            log.warn("Snapshot already exists for problem {}", event.getProblemId());
            return;
        }

        ProblemSnapshot snapshot = ProblemSnapshot.builder()
                .problemId(event.getProblemId())
                .title(event.getTitle())
                .createdAt(event.getOccurredAt())
                .recordStatus(RecordStatus.ACTIVE)
                .ioByLanguage(event.getIoByLanguage())
                .build();

        problemSnapshotRepository.save(snapshot);
        log.info("Snapshot created for problem {}", event.getProblemId());

    }

    private void handleProblemDeletion(ProblemEvent event){
        problemSnapshotRepository.findById(event.getProblemId())
                .ifPresent(snapshot -> {
                    snapshot.setRecordStatus(RecordStatus.DELETED);
                    problemSnapshotRepository.save(snapshot);
                    log.info("Snapshot marked deleted for problem {}", event.getProblemId());
                });

    }
}
