package com.dating.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ScamMonitorUserResponse {

    private Long userId;
    private String email;
    private String nickname;
    private String status;
    private Integer fraudRiskScore;
    private String fraudReviewStatus;
    private String region;
    private String job;
    private long openReportCount;
    private String latestRiskDetail;
}