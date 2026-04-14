/**
 * AuthController API 엔드포인트
 */
package com.dating.backend.controller;

import com.dating.backend.dto.AuthRequest;
import com.dating.backend.dto.AuthResponse;
import com.dating.backend.dto.ForgotPasswordRequest;
import com.dating.backend.dto.MessageResponse;
import com.dating.backend.dto.PasswordResetConfirmRequest;
import com.dating.backend.dto.PasswordResetRequestResponse;
import com.dating.backend.dto.PhoneVerificationConfirmRequest;
import com.dating.backend.dto.PhoneVerificationRequest;
import com.dating.backend.dto.PhoneVerificationStartResponse;
import com.dating.backend.dto.SignupRequest;
import com.dating.backend.service.AuthService;
import com.dating.backend.service.PhoneAuthService;
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

    private final AuthService authService;
    private final PhoneAuthService phoneAuthService;

    @PostMapping("/signup")
    public AuthResponse signup(@Valid @RequestBody SignupRequest request) {
        return authService.signup(request);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody AuthRequest request) {
        return authService.login(request);
    }

    @PostMapping("/phone/request")
    public PhoneVerificationStartResponse requestPhoneVerification(@Valid @RequestBody PhoneVerificationRequest request) {
        return phoneAuthService.requestCode(request);
    }

    @PostMapping("/phone/verify")
    public AuthResponse verifyPhoneAndLogin(@Valid @RequestBody PhoneVerificationConfirmRequest request) {
        return phoneAuthService.verifyAndLogin(request);
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