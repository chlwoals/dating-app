/**
 * PhoneVerificationStartResponse 요청/응답 DTO
 */
package com.dating.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class PhoneVerificationStartResponse {

    // 응답 메시지
    private String message;
    // 만료 시각
    private LocalDateTime expiresAt;
}
