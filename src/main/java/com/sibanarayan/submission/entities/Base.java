package com.sibanarayan.submission.entities;

import com.sibanarayan.submission.enums.RecordStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@MappedSuperclass
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Base {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    public UUID id;

    @Column(name="created_at",nullable = false)
    private Instant createdAt;

    @Column(name="updated_at",nullable = false)
    private Instant updatedAt;

    @Column(name="record_status")
    @Enumerated(EnumType.STRING)
    private RecordStatus recordStatus;

    @PrePersist
    protected void onCreate(){
        recordStatus= RecordStatus.ACTIVE;
        createdAt=updatedAt=Instant.now();
    }
    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}
