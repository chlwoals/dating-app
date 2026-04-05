package com.dating.backend.controller;

import com.dating.backend.dto.AuthRequest;
import com.dating.backend.dto.AuthResponse;
import com.dating.backend.dto.ForgotPasswordRequest;
import com.dating.backend.dto.MessageResponse;
import com.dating.backend.dto.PasswordResetConfirmRequest;
import com.dating.backend.dto.PasswordResetRequestResponse;
import com.dating.backend.dto.SignupRequest;
import com.dating.backend.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    // 인증 관련 실제 비즈니스 로직은 서비스 계층에 모아두고 컨트롤러는 입출력만 담당한다.
    private final AuthService authService;

    @PostMapping("/signup")
    public AuthResponse signup(@Valid @RequestBody SignupRequest request) {
        return authService.signup(request);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody AuthRequest request) {
        return authService.login(request);
    }

    @PostMapping("/password/reset/request")
    public PasswordResetRequestResponse requestPasswordReset(@Valid @RequestBody ForgotPasswordRequest request) {
        return authService.requestPasswordReset(request);
    }

    @PostMapping("/password/reset/confirm")
    public MessageResponse confirmPasswordReset(@Valid @RequestBody PasswordResetConfirmRequest request) {
        return authService.confirmPasswordReset(request);
    }
}
