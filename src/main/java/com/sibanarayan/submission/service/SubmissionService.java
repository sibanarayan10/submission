package com.sibanarayan.submission.service;

import com.sibanarayan.submission.models.request.SubmissionRequest;

import java.util.UUID;

public interface SubmissionService {
    UUID createOrEditSubmission(SubmissionRequest request);


}
