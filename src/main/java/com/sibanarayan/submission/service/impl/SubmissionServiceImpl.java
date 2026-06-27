package com.sibanarayan.submission.service.impl;


import com.sibanarayan.shared_package.enums.RecordStatus;
import com.sibanarayan.shared_package.enums.SubmissionStatus;
import com.sibanarayan.submission.entities.Submission;
import com.sibanarayan.submission.events.SubmissionEvent;
import com.sibanarayan.submission.models.request.SubmissionRequest;
import com.sibanarayan.submission.models.response.SubmissionResponse;
import com.sibanarayan.submission.repositories.ProblemSnapshotRepository;
import com.sibanarayan.submission.repositories.SubmissionRepositories;
import com.sibanarayan.submission.repositories.UserSnapshotRepository;
import com.sibanarayan.submission.service.SubmissionService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.errors.ResourceNotFoundException;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
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
    public List<SubmissionResponse> getSubmissions(UUID userId,UUID problemId){
        validateProblem(problemId);
        return submissionRepositories.findByUserIdAndProblemIdOrderByCreatedAtDesc(userId,problemId)
                .stream().map(this::mapToResponse)
                .toList();
    }



    private SubmissionResponse mapToResponse(Submission submission){
        return SubmissionResponse.builder()
                .createdAt(submission.getCreatedAt())
                .userId(submission.getUserId())
                .problemId(submission.getProblemId())
                .status(submission.getStatus())
                .id(submission.getId())
                .total(submission.getTotal())
                .errorMessage(submission.getErrorMessage())
                .runtimeMs(submission.getRuntimeMs())
                .passed(submission.getPassed())
                .build();
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
                        log.error("Failed to publish {} event for problem {}","submission event",problemId, ex);
                    else
                        log.info("Published {} event for problem {}","submission event",problemId);

                });
    }
    private void validateUserAndProblem(SubmissionRequest request){
      UUID userId=request.getUserId();
      UUID problemId=request.getProblemId();

        validateProblem(problemId);
        validateUser(userId);
      }

  private void validateUser(UUID userId){
      if(!userSnapshotRepository.existsByUserIdAndRecordStatus(userId, RecordStatus.ACTIVE)){
          throw new ResourceNotFoundException("User not found");
      }
  }

  private void validateProblem(UUID problemId){
      if(!problemSnapshotRepository.existsByProblemIdAndRecordStatus(problemId, RecordStatus.ACTIVE)){
          throw new ResourceNotFoundException("Problem not found");
      }
  }
}
