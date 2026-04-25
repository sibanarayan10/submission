package com.sibanarayan.submission.entities;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name="user_snapshot")
@Builder
@Setter
@Getter
public class UserSnapshot extends  Base {
    @Column(name="user_id",nullable = false,updatable = false,insertable = false)
    private UUID userId;

    @Column(name="user_id",nullable = false)
    private UUID email;

    @Column(name="user_name",nullable = false)
    private String userName;
}
