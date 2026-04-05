package com.dating.backend.service;

import com.dating.backend.dto.MyProfileResponse;
import com.dating.backend.dto.UpdateMyProfileRequest;
import com.dating.backend.entity.User;
import com.dating.backend.entity.UserProfile;
import com.dating.backend.entity.UserVerification;
import com.dating.backend.repository.UserProfileRepository;
import com.dating.backend.repository.UserRepository;
import com.dating.backend.repository.UserVerificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ProfileService {

    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final UserVerificationRepository userVerificationRepository;

    // 로그인한 사용자의 계정/프로필/인증 정보를 한 번에 묶어 반환한다.
    @Transactional(readOnly = true)
    public MyProfileResponse getMyProfile(String email) {
        User user = getUserByEmail(email);
        UserProfile profile = userProfileRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "프로필 정보를 찾을 수 없습니다."));
        UserVerification verification = userVerificationRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "인증 정보를 찾을 수 없습니다."));

        return MyProfileResponse.from(user, profile, verification);
    }

    // 내 프로필 수정 화면에서 넘어온 값을 users, user_profiles, user_verifications에 반영한다.
    @Transactional
    public MyProfileResponse updateMyProfile(String email, UpdateMyProfileRequest request) {
        User user = getUserByEmail(email);
        UserProfile profile = userProfileRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "프로필 정보를 찾을 수 없습니다."));
        UserVerification verification = userVerificationRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "인증 정보를 찾을 수 없습니다."));

        user.setNickname(request.getNickname());

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

        verification.setBirthDate(request.getBirthDate());
        verification.setGender(request.getGender());
        verification.setUpdatedAt(LocalDateTime.now());

        userRepository.save(user);
        userProfileRepository.save(profile);
        userVerificationRepository.save(verification);

        return MyProfileResponse.from(user, profile, verification);
    }

    private User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."));
    }

    private String emptyToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value;
    }
}
