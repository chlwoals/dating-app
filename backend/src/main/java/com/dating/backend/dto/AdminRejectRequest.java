package com.dating.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AdminRejectRequest {

    @NotBlank
    @Size(max = 255)
    private String reviewComment;
}
