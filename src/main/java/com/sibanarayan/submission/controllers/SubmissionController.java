package com.sibanarayan.submission.controllers;

import com.sibanarayan.submission.models.request.SubmissionRequest;
import com.sibanarayan.submission.service.SubmissionService;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("api/v1/submission")
@AllArgsConstructor
public class SubmissionController {
    private final SubmissionService submissionService;
    @PostMapping()
    public ResponseEntity<UUID> createSubmission(SubmissionRequest request){
        UUID submissionId=submissionService.createOrEditSubmission(request);
        return  ResponseEntity.ok(submissionId);
    }
}
