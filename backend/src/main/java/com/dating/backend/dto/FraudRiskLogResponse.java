package com.dating.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class FraudRiskLogResponse {

    private Long id;
    private String riskType;
    private Integer score;
    private String detail;
    private LocalDateTime createdAt;
}