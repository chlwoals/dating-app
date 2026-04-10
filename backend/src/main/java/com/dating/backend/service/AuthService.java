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

    // ?뚯썝媛????怨꾩젙怨?湲곕낯 ?꾨줈???몄쬆 ?뺣낫瑜??④퍡 ?앹꽦?쒕떎.
    @Transactional
    public AuthResponse signup(SignupRequest request) {
        validateAdultSignup(request);
        blockedIdentityService.validateSignupAllowed(request.getEmail(), null);

        User existingUser = userRepository.findByEmail(request.getEmail()).orElse(null);

        if (existingUser == null && userRepository.existsByNickname(request.getNickname())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "?대? ?ъ슜 以묒씤 ?됰꽕?꾩엯?덈떎.");
        }

        if (existingUser != null
                && !request.getNickname().equals(existingUser.getNickname())
                && userRepository.existsByNickname(request.getNickname())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "?대? ?ъ슜 以묒씤 ?됰꽕?꾩엯?덈떎.");
        }

        if (existingUser != null) {
            if (hasLocalPassword(existingUser)) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "?대? 媛?낇븳 ?대찓?쇱엯?덈떎. 濡쒓렇?명븯嫄곕굹 鍮꾨?踰덊샇 ?ъ꽕?뺤쓣 ?댁슜?댁＜?몄슂."
                );
            }

            existingUser.setNickname(request.getNickname());
            existingUser.setPassword(passwordEncoder.encode(request.getPassword()));
            existingUser.setProvider(PROVIDER_BOTH);
            existingUser.setStatus("PENDING_REVIEW");
            existingUser.setReviewComment("?꾨줈???ъ쭊 ?ъ궗 ?湲?以묒엯?덈떎. 3???대궡???꾨줈?꾧낵 ?ъ쭊 ?깅줉???꾨즺?댁＜?몄슂.");
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
                .reviewComment("?꾨줈???ъ쭊 ?ъ궗 ?湲?以묒엯?덈떎. 3???대궡???꾨줈?꾧낵 ?ъ쭊 ?깅줉???꾨즺?댁＜?몄슂.")
                .reviewDeadlineAt(accountReviewPolicyService.createSignupDeadline())
                .password(passwordEncoder.encode(request.getPassword()))
                .build();

        User savedUser = userRepository.save(user);
        upsertProfile(savedUser, request);
        upsertVerification(savedUser, request);
        fraudDetectionService.evaluateUserProfile(savedUser.getId());
        return new AuthResponse(jwtUtil.createToken(savedUser.getEmail()), UserResponse.from(savedUser));
    }

    // ?대찓??濡쒓렇???깃났 ??JWT瑜?諛쒓툒?쒕떎.
    @Transactional(readOnly = true)
    public AuthResponse login(AuthRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "가입되지 않은 이메일이거나 비밀번호가 올바르지 않습니다."
                ));

        blockedIdentityService.validateLoginAllowed(user);

        if ("SUSPENDED".equals(user.getStatus())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "운영 정책상 이용이 제한된 계정입니다.");
        }

        if ("DELETED".equals(user.getStatus())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "기한 내 프로필 또는 사진 등록을 완료하지 않아 계정이 정리되었습니다.");
        }

        if ("HIGH_RISK".equals(user.getFraudReviewStatus())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "운영 정책상 이용이 제한된 계정입니다.");
        }

        if (!hasLocalPassword(user)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "이 계정은 소셜 로그인 전용입니다. 동일한 이메일로 비밀번호 연결 후 로그인해주세요."
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

    // 鍮꾨?踰덊샇 ?ъ꽕???좏겙??諛쒓툒?쒕떎.
    @Transactional
    public PasswordResetRequestResponse requestPasswordReset(ForgotPasswordRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "?낅젰???대찓?쇰줈 媛?낇븳 怨꾩젙??李얠쓣 ???놁뒿?덈떎."));

        if (!hasLocalPassword(user)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "??怨꾩젙? ?뚯뀥 濡쒓렇???꾩슜?낅땲?? ?숈씪 ?대찓?쇰줈 鍮꾨?踰덊샇 ?곌껐 ??吏꾪뻾?댁＜?몄슂."
            );
        }

        String token = UUID.randomUUID().toString();
        user.setResetPasswordToken(token);
        user.setResetPasswordTokenExpiresAt(LocalDateTime.now().plusMinutes(30));
        userRepository.save(user);

        return new PasswordResetRequestResponse(
                "鍮꾨?踰덊샇 ?ъ꽕???좏겙??諛쒓툒?덉뒿?덈떎. ?꾩옱??媛쒕컻 ?④퀎???묐떟?쇰줈 諛붾줈 諛섑솚?⑸땲??",
                token
        );
    }

    // ?ъ꽕???좏겙???좏슚?섎㈃ ??鍮꾨?踰덊샇濡?援먯껜?쒕떎.
    @Transactional
    public MessageResponse confirmPasswordReset(PasswordResetConfirmRequest request) {
        User user = userRepository.findByResetPasswordToken(request.getToken())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "?좏슚?섏? ?딆? ?ъ꽕???좏겙?낅땲??"));

        if (user.getResetPasswordTokenExpiresAt() == null
                || user.getResetPasswordTokenExpiresAt().isBefore(LocalDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "?ъ꽕???좏겙??留뚮즺?섏뿀?듬땲?? ?ㅼ떆 ?붿껌?댁＜?몄슂.");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setProvider(linkWithLocalProvider(user.getProvider()));
        clearResetToken(user);
        userRepository.save(user);

        return new MessageResponse("鍮꾨?踰덊샇媛 ?깃났?곸쑝濡?蹂寃쎈릺?덉뒿?덈떎. ??鍮꾨?踰덊샇濡?濡쒓렇?명빐二쇱꽭??");
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
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "留?19???댁긽留?媛?낇븷 ???덉뒿?덈떎.");
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
