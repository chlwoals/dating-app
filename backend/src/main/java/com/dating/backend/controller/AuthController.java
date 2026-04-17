/**
 * AuthController API 엔드포인트
 */
package com.dating.backend.controller;

import com.dating.backend.dto.AuthRequest;
import com.dating.backend.dto.AuthResponse;
import com.dating.backend.dto.AuthVerificationConfirmRequest;
import com.dating.backend.dto.AuthVerificationConfirmResponse;
import com.dating.backend.dto.AuthVerificationRequest;
import com.dating.backend.dto.AuthVerificationStartResponse;
import com.dating.backend.dto.ForgotPasswordRequest;
import com.dating.backend.dto.MessageResponse;
import com.dating.backend.dto.PasswordResetConfirmRequest;
import com.dating.backend.dto.PasswordResetRequestResponse;
import com.dating.backend.dto.PhoneVerificationConfirmRequest;
import com.dating.backend.dto.PhoneVerificationRequest;
import com.dating.backend.dto.PhoneVerificationStartResponse;
import com.dating.backend.dto.SignupRequest;
import com.dating.backend.service.AuthService;
import com.dating.backend.service.AuthVerificationService;
import com.dating.backend.service.PhoneAuthService;
import com.dating.backend.service.RefreshTokenService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private static final String REFRESH_TOKEN_COOKIE_NAME = "refreshToken";

    private final AuthService authService;
    private final PhoneAuthService phoneAuthService;
    private final AuthVerificationService authVerificationService;
    private final RefreshTokenService refreshTokenService;

    @Value("${app.auth.refresh-cookie-secure:false}")
    private boolean refreshCookieSecure;

    @Value("${app.auth.refresh-cookie-max-age-seconds:1209600}")
    private int refreshCookieMaxAgeSeconds;

    @PostMapping("/signup")
    public AuthResponse signup(@Valid @RequestBody SignupRequest request, HttpServletResponse response) {
        return withRefreshCookie(authService.signup(request), response);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody AuthRequest request, HttpServletResponse response) {
        return withRefreshCookie(authService.login(request), response);
    }

    @PostMapping("/phone/request")
    public PhoneVerificationStartResponse requestPhoneVerification(@Valid @RequestBody PhoneVerificationRequest request) {
        return phoneAuthService.requestCode(request);
    }

    @PostMapping("/phone/verify")
    public AuthResponse verifyPhoneAndLogin(
            @Valid @RequestBody PhoneVerificationConfirmRequest request,
            HttpServletResponse response
    ) {
        return withRefreshCookie(phoneAuthService.verifyAndLogin(request), response);
    }

    @PostMapping("/verification/request")
    public AuthVerificationStartResponse requestVerification(@Valid @RequestBody AuthVerificationRequest request) {
        return authVerificationService.requestCode(request);
    }

    @PostMapping("/verification/confirm")
    public AuthVerificationConfirmResponse confirmVerification(@Valid @RequestBody AuthVerificationConfirmRequest request) {
        return authVerificationService.confirmCode(request);
    }

    @PostMapping("/refresh")
    public AuthResponse refresh(HttpServletRequest request, HttpServletResponse response) {
        String refreshToken = extractRefreshTokenCookie(request);
        return withRefreshCookie(refreshTokenService.refresh(refreshToken), response);
    }

    @PostMapping("/password/reset/request")
    public PasswordResetRequestResponse requestPasswordReset(@Valid @RequestBody ForgotPasswordRequest request) {
        return authService.requestPasswordReset(request);
    }

    @PostMapping("/password/reset/confirm")
    public MessageResponse confirmPasswordReset(@Valid @RequestBody PasswordResetConfirmRequest request) {
        return authService.confirmPasswordReset(request);
    }

    private AuthResponse withRefreshCookie(AuthResponse authResponse, HttpServletResponse response) {
        addRefreshTokenCookie(response, authResponse.getRefreshToken());
        authResponse.setRefreshToken(null);
        return authResponse;
    }

    private void addRefreshTokenCookie(HttpServletResponse response, String refreshToken) {
        Cookie cookie = new Cookie(REFRESH_TOKEN_COOKIE_NAME, refreshToken);
        cookie.setHttpOnly(true);
        cookie.setSecure(refreshCookieSecure);
        cookie.setPath("/api/auth");
        cookie.setMaxAge(refreshCookieMaxAgeSeconds);
        cookie.setAttribute("SameSite", refreshCookieSecure ? "None" : "Lax");
        response.addCookie(cookie);
    }

    private String extractRefreshTokenCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            throw new org.springframework.web.server.ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인 유지 토큰이 없습니다.");
        }

        for (Cookie cookie : cookies) {
            if (REFRESH_TOKEN_COOKIE_NAME.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }

        throw new org.springframework.web.server.ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인 유지 토큰이 없습니다.");
    }
}
