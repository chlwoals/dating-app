package com.dating.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ReviewStatusResponse {

    private String status;
    private int imageCount;
    private boolean readyForReview;
    private String reviewComment;
    private String message;
}
