package com.dating.backend.dto;

import com.dating.backend.entity.User;
import com.dating.backend.entity.UserProfile;
import com.dating.backend.entity.UserVerification;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class MyProfileResponse {

    // 프론트 프로필 화면에서 바로 쓰기 좋도록 계정/프로필/인증 정보를 하나로 합친 응답 DTO다.

    private Long userId;
    private String email;
    private String nickname;
    private String status;
    private String provider;
    private String reviewComment;
    private LocalDate birthDate;
    private String gender;
    private Boolean isVerified;
    private LocalDateTime verifiedAt;
    private String region;
    private String job;
    private String mbti;
    private String personality;
    private String idealType;
    private String introduction;
    private String smokingStatus;
    private String drinkingStatus;
    private String religion;

    public static MyProfileResponse from(User user, UserProfile profile, UserVerification verification) {
        return new MyProfileResponse(
                user.getId(),
                user.getEmail(),
                user.getNickname(),
                user.getStatus(),
                user.getProvider(),
                user.getReviewComment(),
                verification.getBirthDate(),
                verification.getGender(),
                verification.getIsVerified(),
                verification.getVerifiedAt(),
                profile.getRegion(),
                profile.getJob(),
                profile.getMbti(),
                profile.getPersonality(),
                profile.getIdealType(),
                profile.getIntroduction(),
                profile.getSmokingStatus(),
                profile.getDrinkingStatus(),
                profile.getReligion()
        );
    }
}
