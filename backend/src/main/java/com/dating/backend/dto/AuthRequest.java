/**
 * AuthRequest 요청/응답 DTO
 */
package com.dating.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AuthRequest {

    @Email
    @NotBlank
    // 이메일
    private String email;

    @NotBlank
    // 비밀번호
    private String password;
}
