package com.sibanarayan.submission.entities;

import com.sibanarayan.submission.enums.ProgrammingLanguage;
import com.sibanarayan.submission.enums.SubmissionStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.UUID;

@Entity
@Table(name="submission")
@SuperBuilder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Submission extends  Base {
    @Column(name="user_id",nullable = false,updatable = false)
    private UUID userId;

    @Column(name="problem_id",nullable = false,updatable = false)
    private UUID problemId;

    @Column(name="programming_language",nullable = false)
    @Enumerated(EnumType.STRING)
    private ProgrammingLanguage language;

    @Column(name="solution")
    private String solution;

    @Column(name="status")
    @Enumerated(EnumType.STRING)
    private SubmissionStatus status;

    @Column(name="error_message")
    private String errorMessage;

    @Column(name="runtime_ms")
    private Integer runtimeMs;

    @Column(name="memory")
    private Integer memoryKb;

}
