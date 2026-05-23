package com.sibanarayan.submission.entities;

import com.sibanarayan.submission.enums.RecordStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name="user_snapshot")
@Setter
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserSnapshot{
    @Column(name="user_id",nullable = false,updatable = false,insertable = false,unique = true)
    @Id
    private UUID userId;

    @Column(name="email",nullable = false)
    private String email;

    @Column(name="name",nullable = false)
    private String name;

    @Column(name="created_at",nullable = false,updatable = false)
    private Instant createdAt;

    @Column(name="record_status")
    @Enumerated(EnumType.STRING)
    private RecordStatus recordStatus;


}
