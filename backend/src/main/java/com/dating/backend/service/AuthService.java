/**
 * AuthService 비즈니스 로직
 */
package com.dating.backend.service;

import com.dating.backend.config.JwtUtil;
import com.dating.backend.dto.AuthRequest;
import com.dating.backend.dto.AuthResponse;
import com.dating.backend.dto.ForgotPasswordRequest;
import com.dating.backend.dto.MessageResponse;
import com.dating.backend.dto.PasswordResetConfirmRequest;
import com.dating.backend.dto.PasswordResetRequestResponse;
import com.dating.backend.dto.SignupRequest;
import com.dating.backend.dto.UserResponse;
import com.dating.backend.entity.User;
import com.dating.backend.entity.UserProfile;
import com.dating.backend.entity.UserVerification;
import com.dating.backend.repository.UserProfileRepository;
import com.dating.backend.repository.UserRepository;
import com.dating.backend.repository.UserVerificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final String PROVIDER_LOCAL = "LOCAL";
    private static final String PROVIDER_GOOGLE = "GOOGLE";
    private static final String PROVIDER_BOTH = "BOTH";

    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final UserVerificationRepository userVerificationRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AccountReviewPolicyService accountReviewPolicyService;
    private final FraudDetectionService fraudDetectionService;
    private final BlockedIdentityService blockedIdentityService;

    @Transactional
    public AuthResponse signup(SignupRequest request) {
        validateAdultSignup(request);
        blockedIdentityService.validateSignupAllowed(request.getEmail(), null);

        User existingUser = userRepository.findByEmail(request.getEmail()).orElse(null);

        if (existingUser == null && userRepository.existsByNickname(request.getNickname())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 사용 중인 닉네임입니다.");
        }

        if (existingUser != null
                && !request.getNickname().equals(existingUser.getNickname())
                && userRepository.existsByNickname(request.getNickname())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 사용 중인 닉네임입니다.");
        }

        if (existingUser != null) {
            if (hasLocalPassword(existingUser)) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "이미 가입된 이메일입니다. 다른 이메일을 사용해 주세요."
                );
            }

            existingUser.setNickname(request.getNickname());
            existingUser.setPassword(passwordEncoder.encode(request.getPassword()));
            existingUser.setProvider(PROVIDER_BOTH);
            existingUser.setStatus("PENDING_REVIEW");
            existingUser.setReviewComment("회원 가입 심사 대기 중입니다. 3일 내에 사진 등록과 프로필 입력을 완료해 주세요.");
            existingUser.setReviewDeadlineAt(accountReviewPolicyService.createSignupDeadline());
            existingUser.setDeletedAt(null);
            clearResetToken(existingUser);

            User linkedUser = userRepository.save(existingUser);
            upsertProfile(linkedUser, request);
            upsertVerification(linkedUser, request);
            fraudDetectionService.evaluateUserProfile(linkedUser.getId());
            return new AuthResponse(jwtUtil.createToken(linkedUser.getEmail()), UserResponse.from(linkedUser));
        }

        User user = User.builder()
                .email(request.getEmail())
                .nickname(request.getNickname())
                .provider(PROVIDER_LOCAL)
                .status("PENDING_REVIEW")
                .reviewComment("회원 가입 심사 대기 중입니다. 3일 내에 사진 등록과 프로필 입력을 완료해 주세요.")
                .reviewDeadlineAt(accountReviewPolicyService.createSignupDeadline())
                .password(passwordEncoder.encode(request.getPassword()))
                .build();

        User savedUser = userRepository.save(user);
        upsertProfile(savedUser, request);
        upsertVerification(savedUser, request);
        fraudDetectionService.evaluateUserProfile(savedUser.getId());
        return new AuthResponse(jwtUtil.createToken(savedUser.getEmail()), UserResponse.from(savedUser));
    }

    @Transactional(readOnly = true)
    public AuthResponse login(AuthRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "이메일 또는 비밀번호가 올바르지 않습니다."
                ));

        blockedIdentityService.validateLoginAllowed(user);

        if ("SUSPENDED".equals(user.getStatus())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "정지된 계정입니다. 고객센터에 문의해 주세요.");
        }

        if ("DELETED".equals(user.getStatus())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "탈퇴 처리된 계정입니다. 새로운 계정으로 가입해 주세요.");
        }

        if ("HIGH_RISK".equals(user.getFraudReviewStatus())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "스캠 의심 계정으로 분류되어 로그인할 수 없습니다.");
        }

        if (!hasLocalPassword(user)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "소셜 가입 계정입니다. 이메일 로그인 대신 소셜 로그인을 이용해 주세요."
            );
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "이메일 또는 비밀번호가 올바르지 않습니다."
            );
        }

        return new AuthResponse(jwtUtil.createToken(user.getEmail()), UserResponse.from(user));
    }

    @Transactional
    public PasswordResetRequestResponse requestPasswordReset(ForgotPasswordRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "가입된 이메일을 찾을 수 없습니다."));

        if (!hasLocalPassword(user)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "소셜 가입 계정은 비밀번호 재설정을 지원하지 않습니다."
            );
        }

        String token = UUID.randomUUID().toString();
        user.setResetPasswordToken(token);
        user.setResetPasswordTokenExpiresAt(LocalDateTime.now().plusMinutes(30));
        userRepository.save(user);

        return new PasswordResetRequestResponse(
                "비밀번호 재설정 토큰을 발급했습니다. 30분 내에 변경을 완료해 주세요.",
                token
        );
    }

    @Transactional
    public MessageResponse confirmPasswordReset(PasswordResetConfirmRequest request) {
        User user = userRepository.findByResetPasswordToken(request.getToken())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "유효하지 않은 비밀번호 재설정 요청입니다."));

        if (user.getResetPasswordTokenExpiresAt() == null
                || user.getResetPasswordTokenExpiresAt().isBefore(LocalDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "비밀번호 재설정 링크가 만료되었습니다. 다시 요청해 주세요.");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setProvider(linkWithLocalProvider(user.getProvider()));
        clearResetToken(user);
        userRepository.save(user);

        return new MessageResponse("비밀번호가 정상적으로 변경되었습니다. 새 비밀번호로 로그인해 주세요.");
    }

    private void upsertProfile(User user, SignupRequest request) {
        UserProfile profile = userProfileRepository.findByUserId(user.getId())
                .orElseGet(() -> UserProfile.builder().userId(user.getId()).build());

        profile.setRegion(request.getRegion());
        profile.setJob(emptyToNull(request.getJob()));
        profile.setMbti(emptyToNull(request.getMbti()));
        profile.setPersonality(emptyToNull(request.getPersonality()));
        profile.setIdealType(emptyToNull(request.getIdealType()));
        profile.setIntroduction(emptyToNull(request.getIntroduction()));
        profile.setSmokingStatus(request.getSmokingStatus());
        profile.setDrinkingStatus(request.getDrinkingStatus());
        profile.setReligion(request.getReligion());
        profile.setUpdatedAt(LocalDateTime.now());

        UserProfile savedProfile = userProfileRepository.save(profile);
        updateProfileCompletedAt(user, savedProfile,
                userVerificationRepository.findByUserId(user.getId()).orElse(null));
    }

    private void upsertVerification(User user, SignupRequest request) {
        UserVerification verification = userVerificationRepository.findByUserId(user.getId())
                .orElseGet(() -> UserVerification.builder().userId(user.getId()).build());

        verification.setBirthDate(request.getBirthDate());
        verification.setGender(request.getGender());
        verification.setIsVerified(false);
        verification.setVerifiedAt(null);
        verification.setUpdatedAt(LocalDateTime.now());

        UserVerification savedVerification = userVerificationRepository.save(verification);
        updateProfileCompletedAt(user,
                userProfileRepository.findByUserId(user.getId()).orElse(null),
                savedVerification);
    }

    private void updateProfileCompletedAt(User user, UserProfile profile, UserVerification verification) {
        if (profile == null || verification == null) {
            return;
        }

        if (accountReviewPolicyService.isProfileComplete(profile, verification)) {
            if (user.getProfileCompletedAt() == null) {
                user.setProfileCompletedAt(LocalDateTime.now());
            }
        } else {
            user.setProfileCompletedAt(null);
        }
        userRepository.save(user);
    }

    private void validateAdultSignup(SignupRequest request) {
        LocalDate adultThreshold = LocalDate.now().minusYears(19);
        if (request.getBirthDate().isAfter(adultThreshold)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "만 19세 미만은 가입할 수 없습니다.");
        }
    }

    private boolean hasLocalPassword(User user) {
        return user.getPassword() != null && !user.getPassword().isBlank();
    }

    private String linkWithLocalProvider(String provider) {
        String normalized = provider == null ? "" : provider.toUpperCase(Locale.ROOT);
        if (PROVIDER_GOOGLE.equals(normalized) || PROVIDER_BOTH.equals(normalized)) {
            return PROVIDER_BOTH;
        }
        return PROVIDER_LOCAL;
    }

    private void clearResetToken(User user) {
        user.setResetPasswordToken(null);
        user.setResetPasswordTokenExpiresAt(null);
    }

    private String emptyToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value;
    }
}
