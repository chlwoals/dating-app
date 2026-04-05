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

    @Email
    @NotBlank
    private String email;

    @NotBlank
    @Size(min = 2, max = 30)
    private String nickname;

    @NotBlank
    @Size(min = 8, max = 100)
    private String password;

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

    @Pattern(regexp = "^$|INTJ|INTP|ENTJ|ENTP|INFJ|INFP|ENFJ|ENFP|ISTJ|ISFJ|ESTJ|ESFJ|ISTP|ISFP|ESTP|ESFP")
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

    @AssertTrue(message = "약관 동의는 필수입니다.")
    private boolean agreedToTerms;
}