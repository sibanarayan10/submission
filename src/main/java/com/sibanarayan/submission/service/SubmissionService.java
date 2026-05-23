package com.sibanarayan.submission.service;

import com.sibanarayan.submission.models.request.SubmissionRequest;
import com.sibanarayan.submission.models.response.SubmissionResponse;

import java.util.List;
import java.util.UUID;

public interface SubmissionService {
    UUID createOrEditSubmission(SubmissionRequest request);
    List<SubmissionResponse> getSubmissions(UUID userId, UUID problemId);

}
