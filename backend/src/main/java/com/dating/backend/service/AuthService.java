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

    // 소개팅 앱 가입 시 계정과 기본 프로필/인증 정보를 함께 만든다.
    @Transactional
    public AuthResponse signup(SignupRequest request) {
        validateAdultSignup(request);

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
                        "이미 가입된 이메일입니다. 로그인하거나 비밀번호 재설정을 이용해주세요."
                );
            }

            existingUser.setNickname(request.getNickname());
            existingUser.setPassword(passwordEncoder.encode(request.getPassword()));
            existingUser.setProvider(PROVIDER_BOTH);
            existingUser.setStatus("PENDING_REVIEW");
            existingUser.setReviewComment("프로필 사진 심사 대기 중입니다. 3일 안에 프로필과 사진 등록을 완료해주세요.");
            existingUser.setReviewDeadlineAt(accountReviewPolicyService.createSignupDeadline());
            existingUser.setDeletedAt(null);
            clearResetToken(existingUser);

            User linkedUser = userRepository.save(existingUser);
            upsertProfile(linkedUser, request);
            upsertVerification(linkedUser, request);
            return new AuthResponse(jwtUtil.createToken(linkedUser.getEmail()), UserResponse.from(linkedUser));
        }

        User user = User.builder()
                .email(request.getEmail())
                .nickname(request.getNickname())
                .provider(PROVIDER_LOCAL)
                .status("PENDING_REVIEW")
                .reviewComment("프로필 사진 심사 대기 중입니다. 3일 안에 프로필과 사진 등록을 완료해주세요.")
                .reviewDeadlineAt(accountReviewPolicyService.createSignupDeadline())
                .password(passwordEncoder.encode(request.getPassword()))
                .build();

        User savedUser = userRepository.save(user);
        upsertProfile(savedUser, request);
        upsertVerification(savedUser, request);
        return new AuthResponse(jwtUtil.createToken(savedUser.getEmail()), UserResponse.from(savedUser));
    }

    // 이메일 로그인 성공 시 JWT를 발급한다.
    @Transactional(readOnly = true)
    public AuthResponse login(AuthRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "가입되지 않은 이메일이거나 비밀번호가 올바르지 않습니다."
                ));

        if ("SUSPENDED".equals(user.getStatus())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "정지된 계정입니다. 고객센터로 문의해주세요.");
        }

        if ("DELETED".equals(user.getStatus())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "기한 내 프로필 또는 사진 등록이 완료되지 않아 계정이 정리되었습니다. 다시 가입해주세요.");
        }

        if (!hasLocalPassword(user)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "이 계정은 Google 로그인만 연결되어 있습니다. 같은 이메일로 회원가입해 비밀번호 로그인을 연결해주세요."
            );
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "가입되지 않은 이메일이거나 비밀번호가 올바르지 않습니다."
            );
        }

        return new AuthResponse(jwtUtil.createToken(user.getEmail()), UserResponse.from(user));
    }

    // 비밀번호 재설정 토큰을 발급한다.
    @Transactional
    public PasswordResetRequestResponse requestPasswordReset(ForgotPasswordRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "입력한 이메일로 가입한 계정을 찾을 수 없습니다."));

        if (!hasLocalPassword(user)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "이 계정은 Google 로그인만 연결되어 있습니다. 같은 이메일로 회원가입해 비밀번호를 먼저 연결해주세요."
            );
        }

        String token = UUID.randomUUID().toString();
        user.setResetPasswordToken(token);
        user.setResetPasswordTokenExpiresAt(LocalDateTime.now().plusMinutes(30));
        userRepository.save(user);

        return new PasswordResetRequestResponse(
                "비밀번호 재설정 토큰을 발급했습니다. 현재는 개발 단계라 응답으로 바로 반환합니다.",
                token
        );
    }

    // 재설정 토큰이 유효하면 새 비밀번호로 교체한다.
    @Transactional
    public MessageResponse confirmPasswordReset(PasswordResetConfirmRequest request) {
        User user = userRepository.findByResetPasswordToken(request.getToken())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "유효하지 않은 재설정 토큰입니다."));

        if (user.getResetPasswordTokenExpiresAt() == null
                || user.getResetPasswordTokenExpiresAt().isBefore(LocalDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "재설정 토큰이 만료되었습니다. 다시 요청해주세요.");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setProvider(linkWithLocalProvider(user.getProvider()));
        clearResetToken(user);
        userRepository.save(user);

        return new MessageResponse("비밀번호가 성공적으로 변경되었습니다. 새 비밀번호로 로그인해주세요.");
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
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "만 19세 이상만 가입할 수 있습니다.");
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