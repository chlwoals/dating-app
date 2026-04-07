package com.dating.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class AdminUserReportResponse {

    private Long id;
    private Long reporterUserId;
    private Long reportedUserId;
    private String reasonCode;
    private String detail;
    private String status;
    private String adminNote;
    private LocalDateTime createdAt;
}