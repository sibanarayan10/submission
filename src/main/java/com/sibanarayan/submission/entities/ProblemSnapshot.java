package com.sibanarayan.submission.entities;

import com.sibanarayan.submission.enums.ProgrammingLanguage;
import com.sibanarayan.submission.enums.RecordStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name="problem_snapshot")
@Setter
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ProblemSnapshot  {

    @Column(name="problem_id",nullable = false)
    @Id
    private UUID problemId;

    @Column(name="created_at",nullable = false)
    private Instant createdAt;

    @Column(name="title",nullable = false)
    private String title;

    @Column(name="runtime_ms")
    private Integer runtimeMs;

    @Column(name="memory")
    private Integer memoryKb;

    @Column(name="record_status")
    @Enumerated(EnumType.STRING)
    private RecordStatus recordStatus;

    @Column(name="io_by_language",columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<ProgrammingLanguage,String> ioByLanguage=new HashMap<>();
}
