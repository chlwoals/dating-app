package com.dating.backend.dto;

import com.dating.backend.entity.User;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class UserResponse {

    private Long id;
    private String email;
    private String nickname;
    private String provider;
    private String status;
    private LocalDateTime reviewDeadlineAt;
    private Integer fraudRiskScore;
    private String fraudReviewStatus;

    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getNickname(),
                user.getProvider(),
                user.getStatus(),
                user.getReviewDeadlineAt(),
                user.getFraudRiskScore(),
                user.getFraudReviewStatus()
        );
    }
}