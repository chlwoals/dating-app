package com.dating.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ScamMonitorSummaryResponse {

    private long highRiskUserCount;
    private long watchUserCount;
    private long openReportCount;
    private long resolvedReportCount;
}