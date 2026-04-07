package com.dating.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class AdminReviewHistoryResponse {

    private Long id;
    private String actionType;
    private String detail;
    private String actorLabel;
    private LocalDateTime createdAt;
}