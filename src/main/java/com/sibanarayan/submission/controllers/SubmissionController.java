package com.sibanarayan.submission.controllers;

import com.sibanarayan.submission.config.JwtFilter;
import com.sibanarayan.submission.models.request.SubmissionRequest;
import com.sibanarayan.submission.models.response.SubmissionResponse;
import com.sibanarayan.submission.service.SubmissionService;
import com.sibanarayan.submission.utility.JwtUtility;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("api/v1/submission")
@AllArgsConstructor
public class SubmissionController {
    private final SubmissionService submissionService;
    private final JwtFilter filter;
    private final JwtUtility utility;
    @PostMapping()
    public ResponseEntity<UUID> createSubmission(@RequestBody  SubmissionRequest request){
        UUID submissionId=submissionService.createOrEditSubmission(request);
        return  ResponseEntity.ok(submissionId);
    }
    @GetMapping()
    public ResponseEntity<List<SubmissionResponse>> getSubmission(@RequestParam UUID problemId, HttpServletRequest request){
        String token=filter.extractTokenFromCookie(request);
        UUID userId=utility.getUserId(token);
        return new ResponseEntity<>(submissionService.getSubmissions(userId,problemId), HttpStatus.ACCEPTED);
    }
}
