package com.sibanarayan.submission.consumers;


import com.sibanarayan.shared_package.enums.EventType;
import com.sibanarayan.shared_package.enums.RecordStatus;
import com.sibanarayan.shared_package.events.UserEvent;
import com.sibanarayan.shared_package.exceptions.EntityAlreadyExistException;
import com.sibanarayan.submission.entities.UserSnapshot;
import com.sibanarayan.submission.repositories.UserSnapshotRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@AllArgsConstructor
@Slf4j
public class UserConsumer {
    private final UserSnapshotRepository userSnapshotRepository;

    @KafkaListener(
            topics = "user.event",
            groupId = "submission-service-user",
            containerFactory = "userEventFactory"
    )
    public void consume(UserEvent event){
        switch(event.getEventType()){
            case EventType.CREATE -> createUserSnapshot(event);
            case EventType.DELETE -> deleteUserSnapshot(event);
        }

    }

    private void createUserSnapshot(UserEvent event){
        if(userSnapshotRepository.existsByEmail(event.getEmail())){
            throw new EntityAlreadyExistException("User with"+ event.getEmail()+"already exist");
        }
        UserSnapshot snapshot=UserSnapshot.builder()
                            .userId(event.getId())
                            .email(event.getEmail())
                            .createdAt(event.getOccurredAt())
                            .name(event.getName())
                            .recordStatus(RecordStatus.ACTIVE)
                            .build();
        userSnapshotRepository.save(snapshot);
        log.info("User snapshot created with user id {}",snapshot.getUserId());

    }

    private void deleteUserSnapshot(UserEvent event){
        UUID userId=event.getId();
        userSnapshotRepository.findByUserId(userId).ifPresent((user)->{
            user.setRecordStatus(RecordStatus.DELETED);
            userSnapshotRepository.save(user);
        });
        log.info("User-{} got deleted",event.getName());
    }

}
