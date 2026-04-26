package com.sibanarayan.submission.service.impl;

import com.sibanarayan.submission.entities.Submission;
import com.sibanarayan.submission.enums.RecordStatus;
import com.sibanarayan.submission.enums.SubmissionStatus;
import com.sibanarayan.submission.events.SubmissionEvent;
import com.sibanarayan.submission.exceptions.EntityNotFoundException;
import com.sibanarayan.submission.models.request.SubmissionRequest;
import com.sibanarayan.submission.repositories.ProblemSnapshotRepository;
import com.sibanarayan.submission.repositories.SubmissionRepositories;
import com.sibanarayan.submission.repositories.UserSnapshotRepository;
import com.sibanarayan.submission.service.SubmissionService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@Slf4j
@AllArgsConstructor
public class SubmissionServiceImpl implements SubmissionService {

    private SubmissionRepositories submissionRepositories;
    private UserSnapshotRepository userSnapshotRepository;
    private ProblemSnapshotRepository problemSnapshotRepository;
    private KafkaTemplate<String, SubmissionEvent> kafkaTemplate;

    private final String SUBMISSION_TOPIC="submission.pending";
    public UUID createOrEditSubmission(SubmissionRequest request){
        Submission submission=new Submission();
        validateUserAndProblem(request);

        if(request.getId()==null){
             submission= Submission.builder().userId(request.getUserId())
                    .problemId(request.getProblemId())
                    .solution(request.getSolution())
                    .language(request.getLanguage())
                     .status(SubmissionStatus.PENDING)
                    .build();
            submissionRepositories.save(submission);
            publishEvent(submission,request.getProblemId());
            log.info("Submission created successfully");
        }

        return submission.getId();
    }
    private void publishEvent(Submission submission,UUID problemId){
        SubmissionEvent event=SubmissionEvent.builder().
                            problemId(problemId)
                                    .language(submission.getLanguage())
                                            .solution(submission.getSolution())
                                                 .submissionId(submission.getId())
                                                    .build();
        kafkaTemplate.send(SUBMISSION_TOPIC, submission.toString(), event)
                .whenComplete((result, ex) -> {
                    if (ex != null)
                        log.error("Failed to publish {} event for problem {}",problemId, ex);
                    else
                        log.info("Published {} event for problem {}",problemId);

                });
    }
  private void validateUserAndProblem(SubmissionRequest request){
      UUID userId=request.getUserId();
      UUID problemId=request.getProblemId();

//      if(!userSnapshotRepository.existsByIdAndRecordStatus(userId,RecordStatus.ACTIVE)){
//          throw new EntityNotFoundException("User not found");
//      }
      if(!problemSnapshotRepository.existsByProblemIdAndRecordStatus(problemId,RecordStatus.ACTIVE)){
          throw new EntityNotFoundException("Problem not found");
      }
  }
}
