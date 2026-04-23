/**
 * PhoneAuthService 비즈니스 로직
 */
package com.dating.backend.service;

import com.dating.backend.config.JwtUtil;
import com.dating.backend.dto.AuthResponse;
import com.dating.backend.dto.PhoneVerificationConfirmRequest;
import com.dating.backend.dto.PhoneVerificationRequest;
import com.dating.backend.dto.PhoneVerificationStartResponse;
import com.dating.backend.dto.UserResponse;
import com.dating.backend.entity.PhoneVerification;
import com.dating.backend.entity.User;
import com.dating.backend.exception.FocusableResponseStatusException;
import com.dating.backend.repository.PhoneVerificationRepository;
import com.dating.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class PhoneAuthService {

    private static final String PROVIDER_PHONE = "PHONE";
    private static final int CODE_TTL_MINUTES = 5;
    private static final int MAX_ATTEMPTS = 5;
    private static final int MAX_REQUESTS_PER_WINDOW = 3;
    private static final int REQUEST_WINDOW_MINUTES = 10;

    private final PhoneVerificationRepository phoneVerificationRepository;
    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final AccountReviewPolicyService accountReviewPolicyService;
    private final BlockedIdentityService blockedIdentityService;
    private final RefreshTokenService refreshTokenService;
    private final PhoneSmsSender phoneSmsSender;
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${app.phone-auth.expose-dev-code:false}")
    private boolean exposeDevCode;

    @Transactional
    public PhoneVerificationStartResponse requestCode(PhoneVerificationRequest request) {
        String phone = normalizePhone(request.getPhone());
        blockedIdentityService.validateSignupAllowed(null, phone);
        validateRequestRateLimit(phone);

        String code = String.format("%06d", secureRandom.nextInt(1_000_000));
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(CODE_TTL_MINUTES);

        phoneVerificationRepository.save(PhoneVerification.builder()
                .phone(phone)
                .code(code)
                .expiresAt(expiresAt)
                .build());

        phoneSmsSender.sendVerificationCode(phone, code, expiresAt);

        return new PhoneVerificationStartResponse(
                "SMS 인증번호가 발송되었습니다. 개발 환경에서는 서버 로그의 [DEV SMS AUTH] 항목에서 코드를 확인할 수 있습니다.",
                expiresAt,
                exposeDevCode ? code : null
        );
    }

    @Transactional
    public AuthResponse verifyAndLogin(PhoneVerificationConfirmRequest request) {
        String phone = normalizePhone(request.getPhone());
        PhoneVerification verification = phoneVerificationRepository.findTopByPhoneOrderByCreatedAtDesc(phone)
                .orElseThrow(() -> new FocusableResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "인증번호를 먼저 요청해 주세요.",
                        "phone"
                ));

        if (Boolean.TRUE.equals(verification.getVerified())) {
            throw new FocusableResponseStatusException(HttpStatus.BAD_REQUEST, "이미 인증이 완료된 요청입니다. 새 인증번호를 요청해 주세요.", "phone");
        }

        if (verification.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new FocusableResponseStatusException(HttpStatus.BAD_REQUEST, "인증번호가 만료되었습니다. 새 인증번호를 요청해 주세요.", "phone");
        }

        if (verification.getAttemptCount() >= MAX_ATTEMPTS) {
            throw new FocusableResponseStatusException(HttpStatus.BAD_REQUEST, "인증 시도 횟수를 초과했습니다. 새 인증번호를 요청해 주세요.", "phone");
        }

        verification.setAttemptCount(verification.getAttemptCount() + 1);
        if (!verification.getCode().equals(request.getCode())) {
            phoneVerificationRepository.save(verification);
            throw new FocusableResponseStatusException(HttpStatus.BAD_REQUEST, "인증번호가 올바르지 않습니다. 다시 확인해 주세요.", "code");
        }

        verification.setVerified(true);
        verification.setVerifiedAt(LocalDateTime.now());
        phoneVerificationRepository.save(verification);

        User user = userRepository.findByPhone(phone).orElseGet(() -> createPhoneUser(phone));
        blockedIdentityService.validateLoginAllowed(user);

        if ("SUSPENDED".equals(user.getStatus()) || "DELETED".equals(user.getStatus()) || "HIGH_RISK".equals(user.getFraudReviewStatus())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "계정 상태로 인해 로그인할 수 없습니다.");
        }

        return new AuthResponse(
                jwtUtil.createToken(user.getEmail()),
                refreshTokenService.issueRefreshToken(user),
                UserResponse.from(user)
        );
    }

    private User createPhoneUser(String phone) {
        User user = User.builder()
                .email(buildPhoneEmail(phone))
                .nickname(buildPhoneNickname(phone))
                .phone(phone)
                .provider(PROVIDER_PHONE)
                .status("PENDING_REVIEW")
                .reviewComment("전화번호 인증이 완료되었습니다. 3일 내에 사진 등록과 프로필 입력을 완료해 심사를 진행해 주세요.")
                .reviewDeadlineAt(accountReviewPolicyService.createSignupDeadline())
                .build();

        return userRepository.save(user);
    }

    private String normalizePhone(String phone) {
        return phone == null ? "" : phone.replaceAll("[^0-9]", "");
    }

    private void validateRequestRateLimit(String phone) {
        LocalDateTime windowStart = LocalDateTime.now().minusMinutes(REQUEST_WINDOW_MINUTES);
        long recentRequestCount = phoneVerificationRepository.countByPhoneAndCreatedAtAfter(phone, windowStart);
        if (recentRequestCount >= MAX_REQUESTS_PER_WINDOW) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "인증번호 요청이 너무 많습니다. 잠시 후 다시 시도해 주세요.");
        }
    }

    private String buildPhoneEmail(String phone) {
        return "phone_" + phone + "@phone.local";
    }

    private String buildPhoneNickname(String phone) {
        return "phone_" + phone;
    }
}
