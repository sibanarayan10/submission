package com.sibanarayan.submission.entities;

import com.sibanarayan.submission.enums.RecordStatus;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name="problem_snapshot")
@Builder
@Setter
@Getter
public class ProblemSnapshot extends Base {
    @Column(name="problem_id",nullable = false)
    private UUID problemId;

    @Column(name="title",nullable = false)
    private String title;

}
