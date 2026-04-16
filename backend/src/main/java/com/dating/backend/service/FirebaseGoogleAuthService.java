/**
 * Firebase Google ID 토큰을 검증하고 우리 서비스 계정으로 연결하는 서비스
 */
package com.dating.backend.service;

import com.dating.backend.config.JwtUtil;
import com.dating.backend.dto.AuthResponse;
import com.dating.backend.dto.UserResponse;
import com.dating.backend.entity.User;
import com.dating.backend.repository.UserRepository;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Locale;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class FirebaseGoogleAuthService {

    private static final String PROVIDER_LOCAL = "LOCAL";
    private static final String PROVIDER_GOOGLE = "GOOGLE";
    private static final String PROVIDER_BOTH = "BOTH";

    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final AccountReviewPolicyService accountReviewPolicyService;
    private final BlockedIdentityService blockedIdentityService;

    @Value("${app.firebase.enabled:false}")
    private boolean firebaseEnabled;

    @Value("${app.firebase.service-account-path:}")
    private String serviceAccountPath;

    @Transactional
    public AuthResponse loginWithGoogle(String idToken) {
        FirebaseToken token = verifyIdToken(idToken);
        String email = token.getEmail();

        if (email == null || email.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Google 계정에서 이메일을 확인할 수 없습니다.");
        }

        blockedIdentityService.validateSignupAllowed(email, null);
        User user = userRepository.findByEmail(email)
                .map(existingUser -> linkGoogleProvider(existingUser, token))
                .orElseGet(() -> createGoogleUser(token));

        blockedIdentityService.validateLoginAllowed(user);
        validateLoginStatus(user);

        return new AuthResponse(jwtUtil.createToken(user.getEmail()), UserResponse.from(user));
    }

    private FirebaseToken verifyIdToken(String idToken) {
        if (!firebaseEnabled) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Firebase Google 로그인이 아직 설정되지 않았습니다. app.firebase.enabled와 서비스 계정 설정을 확인해 주세요."
            );
        }

        try {
            FirebaseApp firebaseApp = getOrInitializeFirebaseApp();
            return FirebaseAuth.getInstance(firebaseApp).verifyIdToken(idToken);
        } catch (IOException exception) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Firebase 서비스 계정 설정을 확인해 주세요."
            );
        } catch (Exception exception) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Google 로그인 인증에 실패했습니다.");
        }
    }

    private FirebaseApp getOrInitializeFirebaseApp() throws IOException {
        Optional<FirebaseApp> existingApp = FirebaseApp.getApps().stream().findFirst();
        if (existingApp.isPresent()) {
            return existingApp.get();
        }

        FirebaseOptions.Builder optionsBuilder = FirebaseOptions.builder();
        if (serviceAccountPath != null && !serviceAccountPath.isBlank()) {
            try (FileInputStream serviceAccount = new FileInputStream(serviceAccountPath)) {
                optionsBuilder.setCredentials(GoogleCredentials.fromStream(serviceAccount));
            }
        } else {
            optionsBuilder.setCredentials(GoogleCredentials.getApplicationDefault());
        }

        return FirebaseApp.initializeApp(optionsBuilder.build());
    }

    private User linkGoogleProvider(User user, FirebaseToken token) {
        user.setProvider(resolveLinkedProvider(user.getProvider()));
        user.setDeletedAt(null);

        if ("DELETED".equals(user.getStatus())) {
            user.setStatus("PENDING_REVIEW");
            user.setReviewDeadlineAt(accountReviewPolicyService.createSignupDeadline());
            user.setReviewComment("Google 로그인으로 계정이 복구되었습니다. 프로필과 사진을 보완해 심사를 진행해 주세요.");
        }

        if (user.getNickname() == null || user.getNickname().isBlank()) {
            user.setNickname(buildUniqueNickname(token));
        }

        return userRepository.save(user);
    }

    private User createGoogleUser(FirebaseToken token) {
        String email = token.getEmail();
        User user = User.builder()
                .email(email)
                .nickname(buildUniqueNickname(token))
                .provider(PROVIDER_GOOGLE)
                .status("PENDING_REVIEW")
                .reviewComment("Google 로그인 가입이 완료되었습니다. 3일 내에 프로필 입력과 사진 등록을 완료해 심사를 진행해 주세요.")
                .reviewDeadlineAt(accountReviewPolicyService.createSignupDeadline())
                .build();

        return userRepository.save(user);
    }

    private void validateLoginStatus(User user) {
        if ("SUSPENDED".equals(user.getStatus())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "정지된 계정입니다. 고객센터에 문의해 주세요.");
        }
        if ("DELETED".equals(user.getStatus())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "탈퇴 처리된 계정입니다. 새로운 계정으로 가입해 주세요.");
        }
        if ("HIGH_RISK".equals(user.getFraudReviewStatus())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "스캠 의심 계정으로 분류되어 로그인할 수 없습니다.");
        }
    }

    private String resolveLinkedProvider(String provider) {
        String normalized = provider == null ? "" : provider.toUpperCase(Locale.ROOT);
        if (PROVIDER_LOCAL.equals(normalized) || PROVIDER_BOTH.equals(normalized)) {
            return PROVIDER_BOTH;
        }
        return PROVIDER_GOOGLE;
    }

    private String buildUniqueNickname(FirebaseToken token) {
        String baseNickname = sanitizeNickname(token.getName());
        if (baseNickname.isBlank()) {
            baseNickname = "google";
        }

        String nickname = baseNickname;
        int suffix = 1;
        while (userRepository.existsByNickname(nickname)) {
            nickname = baseNickname + suffix;
            suffix += 1;
        }
        return nickname;
    }

    private String sanitizeNickname(String value) {
        String normalized = value == null ? "" : value.replaceAll("[^0-9A-Za-z가-힣_-]", "").trim();
        if (normalized.length() > 20) {
            return normalized.substring(0, 20);
        }
        return normalized;
    }
}
