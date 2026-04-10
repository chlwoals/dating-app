package com.dating.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class BlockedIdentityResponse {

    private Long id;
    private String identityType;
    private String identityValue;
    private Long sourceUserId;
    private String sourceUserEmail;
    private String sourceUserNickname;
    private String reason;
    private Boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
