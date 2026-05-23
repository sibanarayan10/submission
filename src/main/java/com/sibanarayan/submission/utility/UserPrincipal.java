package com.sibanarayan.submission.utility;

import lombok.*;

import java.util.UUID;

@Builder
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class UserPrincipal {
    private UUID userId;
    private String email;
    private String role;
}