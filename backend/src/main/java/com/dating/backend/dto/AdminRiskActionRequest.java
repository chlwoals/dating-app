package com.dating.backend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AdminRiskActionRequest {

    @NotBlank(message = "처리 액션을 선택해주세요.")
    private String action;

    @NotBlank(message = "운영 메모를 입력해주세요.")
    private String adminNote;
}