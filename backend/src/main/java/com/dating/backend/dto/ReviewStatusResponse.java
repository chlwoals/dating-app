package com.dating.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class ReviewStatusResponse {

    private String status;
    private int imageCount;
    private boolean readyForReview;
    private boolean profileComplete;
    private String reviewComment;
    private String message;
    private LocalDateTime reviewDeadlineAt;
    private long remainingDays;
}