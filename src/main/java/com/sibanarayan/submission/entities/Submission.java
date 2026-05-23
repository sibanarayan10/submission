package com.sibanarayan.submission.entities;

import com.sibanarayan.submission.enums.ProgrammingLanguage;
import com.sibanarayan.submission.enums.RecordStatus;
import com.sibanarayan.submission.enums.SubmissionStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name="submission")
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Submission {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    public UUID id;

    @Column(name="created_at",nullable = false)
    private Instant createdAt;

    @Column(name="updated_at",nullable = false)
    private Instant updatedAt;

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

    @Column(name="total")
    private Integer total ;

    @Column(name="passed")
    private Integer passed;

    @PrePersist
    protected void onCreate(){
        createdAt=updatedAt=Instant.now();
    }
    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

}
