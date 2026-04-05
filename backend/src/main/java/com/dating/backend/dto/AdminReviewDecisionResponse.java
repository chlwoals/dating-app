package com.dating.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AdminReviewDecisionResponse {

    private Long userId;
    private String status;
    private String reviewComment;
    private String message;
}
