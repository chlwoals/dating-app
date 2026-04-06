package com.dating.backend.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class SignupRequest {

    @Email(message = "올바른 이메일 형식이 아닙니다.")
    @NotBlank(message = "이메일은 필수입니다.")
    private String email;

    @NotBlank(message = "닉네임은 필수입니다.")
    @Size(min = 2, max = 30, message = "닉네임은 2자 이상 30자 이하로 입력해주세요.")
    private String nickname;

    @NotBlank(message = "비밀번호는 필수입니다.")
    @Size(min = 8, max = 100, message = "비밀번호는 8자 이상으로 입력해주세요.")
    private String password;

    @NotNull(message = "생년월일은 필수입니다.")
    private LocalDate birthDate;

    @NotBlank(message = "성별은 필수입니다.")
    @Pattern(regexp = "MALE|FEMALE", message = "성별 값이 올바르지 않습니다.")
    private String gender;

    @NotBlank(message = "거주 지역은 필수입니다.")
    @Size(max = 50, message = "거주 지역은 50자 이하로 입력해주세요.")
    private String region;

    @Size(max = 100, message = "직업은 100자 이하로 입력해주세요.")
    private String job;

    @Pattern(
            regexp = "^$|INTJ|INTP|ENTJ|ENTP|INFJ|INFP|ENFJ|ENFP|ISTJ|ISFJ|ESTJ|ESFJ|ISTP|ISFP|ESTP|ESFP",
            message = "MBTI 값이 올바르지 않습니다."
    )
    private String mbti;

    private String personality;
    private String idealType;
    private String introduction;

    @NotBlank(message = "흡연 여부는 필수입니다.")
    @Pattern(regexp = "NON_SMOKER|SMOKER|OCCASIONAL", message = "흡연 여부 값이 올바르지 않습니다.")
    private String smokingStatus;

    @NotBlank(message = "음주 여부는 필수입니다.")
    @Pattern(regexp = "NONE|SOMETIMES|OFTEN", message = "음주 여부 값이 올바르지 않습니다.")
    private String drinkingStatus;

    @NotBlank(message = "종교는 필수입니다.")
    @Pattern(regexp = "NONE|CHRISTIAN|BUDDHIST|CATHOLIC|OTHER", message = "종교 값이 올바르지 않습니다.")
    private String religion;

    @AssertTrue(message = "약관 동의는 필수입니다.")
    private boolean agreedToTerms;
}