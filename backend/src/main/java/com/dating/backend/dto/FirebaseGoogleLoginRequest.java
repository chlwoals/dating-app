/**
 * Firebase Google 로그인 요청 DTO
 */
package com.dating.backend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FirebaseGoogleLoginRequest {

    // Firebase에서 발급한 ID 토큰
    @NotBlank
    private String idToken;
}
