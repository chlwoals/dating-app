/**
 * ApiErrorResponse 요청/응답 DTO
 */
package com.dating.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ApiErrorResponse {

    // 에러 코드
    private String code;
    // 에러 메시지
    private String message;
}
