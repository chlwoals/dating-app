package com.dating.backend.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserProfileImageRequest {

    @NotBlank
    @Size(max = 500)
    private String imageUrl;

    @NotNull
    @Min(1)
    @Max(5)
    private Integer imageOrder;

    @NotNull
    private Boolean isMain;
}
