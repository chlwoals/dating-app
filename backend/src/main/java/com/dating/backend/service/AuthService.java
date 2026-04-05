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

    // ?뚭컻?????뚯썝媛?낆뿉 ?꾩슂??湲곕낯 ?꾨줈???뺣낫源뚯? ??踰덉뿉 諛쏆븘 怨꾩젙/?꾨줈???몄쬆 ?뺣낫瑜??④퍡 ?앹꽦?쒕떎.
    @Transactional
    public AuthResponse signup(SignupRequest request) {
        validateAdultSignup(request);

        User existingUser = userRepository.findByEmail(request.getEmail()).orElse(null);

        if (existingUser != null) {
            if (hasLocalPassword(existingUser)) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "?대? ?대찓???뚯썝媛?낆씠 ?꾨즺??怨꾩젙?낅땲?? 濡쒓렇???먮뒗 鍮꾨?踰덊샇 ?ъ꽕?뺤쓣 ?댁슜?댁＜?몄슂."
                );
            }

            existingUser.setNickname(request.getNickname());
            existingUser.setPassword(passwordEncoder.encode(request.getPassword()));
            existingUser.setProvider(PROVIDER_BOTH);
            existingUser.setStatus("PENDING_REVIEW");
            existingUser.setReviewComment("?꾨줈???ъ쭊 ?ъ궗 ?湲?以묒엯?덈떎.");
            clearResetToken(existingUser);

            User linkedUser = userRepository.save(existingUser);
            upsertProfile(linkedUser.getId(), request);
            upsertVerification(linkedUser.getId(), request);
            return new AuthResponse(jwtUtil.createToken(linkedUser.getEmail()), UserResponse.from(linkedUser));
        }

        User user = userRepository.save(User.builder()
                .email(request.getEmail())
                .nickname(request.getNickname())
                .provider(PROVIDER_LOCAL)
                .status("PENDING_REVIEW")
                .reviewComment("?꾨줈???ъ쭊 ?ъ궗 ?湲?以묒엯?덈떎.")
                .password(passwordEncoder.encode(request.getPassword()))
                .build());

        upsertProfile(user.getId(), request);
        upsertVerification(user.getId(), request);
        return new AuthResponse(jwtUtil.createToken(user.getEmail()), UserResponse.from(user));
    }

    // ?대찓??+ 鍮꾨?踰덊샇濡?濡쒓렇?명븯怨?JWT瑜?諛쒓툒?쒕떎.
    @Transactional(readOnly = true)
    public AuthResponse login(AuthRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "媛?낅릺吏 ?딆? ?대찓?쇱씠嫄곕굹 鍮꾨?踰덊샇媛 ?щ컮瑜댁? ?딆뒿?덈떎."
                ));

                // 반려/심사대기 계정은 다시 사진을 올릴 수 있어야 하므로 로그인은 허용하고,
        // 프론트에서 review-pending 화면으로 안내한다.
        if ("SUSPENDED".equals(user.getStatus())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "정지된 계정입니다. 고객센터에 문의해주세요."
            );
        }

        if ("DELETED".equals(user.getStatus())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "삭제된 계정입니다. 다시 가입해주세요."
            );
        }

        if (!hasLocalPassword(user)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "??怨꾩젙? Google 濡쒓렇?몃쭔 ?곌껐?섏뼱 ?덉뒿?덈떎. 媛숈? ?대찓?쇰줈 ?뚯썝媛?낇빐 鍮꾨?踰덊샇瑜??곌껐?댁＜?몄슂."
            );
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "媛?낅릺吏 ?딆? ?대찓?쇱씠嫄곕굹 鍮꾨?踰덊샇媛 ?щ컮瑜댁? ?딆뒿?덈떎."
            );
        }

        return new AuthResponse(jwtUtil.createToken(user.getEmail()), UserResponse.from(user));
    }

    // 鍮꾨?踰덊샇 ?ъ꽕?뺤슜 ?좏겙??諛쒓툒??users ?뚯씠釉붿뿉 ??ν븳??
    @Transactional
    public PasswordResetRequestResponse requestPasswordReset(ForgotPasswordRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "?낅젰???대찓?쇰줈 媛?낅맂 怨꾩젙??李얠쓣 ???놁뒿?덈떎."
                ));

        if (!hasLocalPassword(user)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "??怨꾩젙? Google 濡쒓렇?몃쭔 ?곌껐?섏뼱 ?덉뒿?덈떎. 媛숈? ?대찓?쇰줈 ?뚯썝媛?낇빐 鍮꾨?踰덊샇瑜?癒쇱? ?곌껐?댁＜?몄슂."
            );
        }

        String token = UUID.randomUUID().toString();
        user.setResetPasswordToken(token);
        user.setResetPasswordTokenExpiresAt(LocalDateTime.now().plusMinutes(30));
        userRepository.save(user);

        return new PasswordResetRequestResponse(
                "鍮꾨?踰덊샇 ?ъ꽕???좏겙??諛쒓툒?섏뿀?듬땲?? ?꾩옱??媛쒕컻 ?④퀎???묐떟?쇰줈 ?④퍡 諛섑솚?⑸땲??",
                token
        );
    }

    // ?ъ꽕???좏겙???좏슚?섎㈃ ??鍮꾨?踰덊샇濡?援먯껜?쒕떎.
    @Transactional
    public MessageResponse confirmPasswordReset(PasswordResetConfirmRequest request) {
        User user = userRepository.findByResetPasswordToken(request.getToken())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "?좏슚?섏? ?딆? ?ъ꽕???좏겙?낅땲??"
                ));

        if (user.getResetPasswordTokenExpiresAt() == null
                || user.getResetPasswordTokenExpiresAt().isBefore(LocalDateTime.now())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "?ъ꽕???좏겙??留뚮즺?섏뿀?듬땲?? ?ㅼ떆 ?붿껌?댁＜?몄슂."
            );
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setProvider(linkWithLocalProvider(user.getProvider()));
        clearResetToken(user);
        userRepository.save(user);

        return new MessageResponse("鍮꾨?踰덊샇媛 ?깃났?곸쑝濡?蹂寃쎈릺?덉뒿?덈떎. ??鍮꾨?踰덊샇濡?濡쒓렇?명빐二쇱꽭??");
    }

    // ?뚯썝媛?????꾨줈??蹂몃Ц??諛붾줈 梨꾩썙?? 媛??吏곹썑?먮룄 ?뚭컻???쒕퉬?ㅼ뿉???????덈뒗 ?곹깭瑜?留뚮뱺??
    private void upsertProfile(Long userId, SignupRequest request) {
        UserProfile profile = userProfileRepository.findByUserId(userId)
                .orElseGet(() -> UserProfile.builder().userId(userId).build());

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

        userProfileRepository.save(profile);
    }

    // ?뚯썝媛????湲곕낯 ?좎썝 ?뺣낫???④퍡 ??ν빐 ?댄썑 ?몄쬆 ?④퀎? ?곌껐?섍린 ?쎄쾶 留뚮뱺??
    private void upsertVerification(Long userId, SignupRequest request) {
        UserVerification verification = userVerificationRepository.findByUserId(userId)
                .orElseGet(() -> UserVerification.builder().userId(userId).build());

        verification.setBirthDate(request.getBirthDate());
        verification.setGender(request.getGender());
        verification.setIsVerified(false);
        verification.setVerifiedAt(null);
        verification.setUpdatedAt(LocalDateTime.now());

        userVerificationRepository.save(verification);
    }

    // ?뚭컻?????뱀꽦??誘몄꽦??媛?낆? 留됰뒗 履쎌쑝濡?湲곕낯 ?뺤콉???붾떎.
    private void validateAdultSignup(SignupRequest request) {
        if (request.getBirthDate().isAfter(java.time.LocalDate.now().minusYears(19))) {
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
