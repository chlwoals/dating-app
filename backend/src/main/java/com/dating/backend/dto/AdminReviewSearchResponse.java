package com.dating.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AdminReviewSearchResponse {

    private String status;
    private boolean dueSoonOnly;
    private String query;
}