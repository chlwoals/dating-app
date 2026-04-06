package com.dating.backend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AdminMemoRequest {

    @NotBlank(message = "운영자 메모를 입력해주세요.")
    private String adminMemo;
}