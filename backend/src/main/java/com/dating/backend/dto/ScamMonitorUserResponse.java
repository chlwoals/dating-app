package com.dating.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class ScamMonitorUserResponse {

    private Long userId;
    private String email;
    private String nickname;
    private String provider;
    private String status;
    private Integer fraudRiskScore;
    private String fraudReviewStatus;
    private String reviewComment;
    private String adminMemo;
    private String region;
    private String job;
    private long openReportCount;
    private String latestRiskDetail;
    private LocalDateTime createdAt;
}