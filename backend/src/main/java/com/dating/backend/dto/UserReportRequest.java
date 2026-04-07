package com.dating.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserReportRequest {

    @NotNull(message = "신고 대상 회원 ID가 필요합니다.")
    private Long reportedUserId;

    @NotBlank(message = "신고 사유를 선택해주세요.")
    private String reasonCode;

    @NotBlank(message = "상세 설명을 입력해주세요.")
    private String detail;
}