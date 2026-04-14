/**
 * AuthResponse 요청/응답 DTO
 */
package com.dating.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AuthResponse {

// ?醫뤾쿃
    private String token;
// ?袁⑤굡 揶?
    private UserResponse user;
}