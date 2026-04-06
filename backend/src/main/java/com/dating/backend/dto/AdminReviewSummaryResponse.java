package com.dating.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AdminReviewSummaryResponse {

    private long pendingReviewCount;
    private long rejectedCount;
    private long activeCount;
    private long dueSoonCount;
}