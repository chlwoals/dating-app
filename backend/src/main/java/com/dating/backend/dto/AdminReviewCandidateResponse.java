package com.dating.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@AllArgsConstructor
public class AdminReviewCandidateResponse {

    private Long userId;
    private String email;
    private String nickname;
    private String status;
    private String reviewComment;
    private LocalDateTime createdAt;
    private LocalDate birthDate;
    private String gender;
    private String region;
    private String job;
    private String mbti;
    private String introduction;
    private List<UserProfileImageResponse> images;
}
