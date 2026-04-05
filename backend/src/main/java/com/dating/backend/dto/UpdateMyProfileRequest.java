package com.dating.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class UpdateMyProfileRequest {

    // 내 프로필 수정 API에서 받는 입력값 DTO다.

    @NotBlank
    @Size(max = 30)
    private String nickname;

    @NotNull
    private LocalDate birthDate;

    @NotBlank
    @Pattern(regexp = "MALE|FEMALE")
    private String gender;

    @NotBlank
    @Size(max = 50)
    private String region;

    @Size(max = 100)
    private String job;

    @Pattern(
            regexp = "^$|INTJ|INTP|ENTJ|ENTP|INFJ|INFP|ENFJ|ENFP|ISTJ|ISFJ|ESTJ|ESFJ|ISTP|ISFP|ESTP|ESFP"
    )
    private String mbti;

    private String personality;
    private String idealType;
    private String introduction;

    @NotBlank
    @Pattern(regexp = "NON_SMOKER|SMOKER|OCCASIONAL")
    private String smokingStatus;

    @NotBlank
    @Pattern(regexp = "NONE|SOMETIMES|OFTEN")
    private String drinkingStatus;

    @NotBlank
    @Pattern(regexp = "NONE|CHRISTIAN|BUDDHIST|CATHOLIC|OTHER")
    private String religion;
}
