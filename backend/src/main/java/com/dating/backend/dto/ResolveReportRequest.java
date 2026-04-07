package com.dating.backend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ResolveReportRequest {

    @NotBlank(message = "처리 메모를 입력해주세요.")
    private String adminNote;
}