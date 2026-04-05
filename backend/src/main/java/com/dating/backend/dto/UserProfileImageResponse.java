package com.dating.backend.dto;

import com.dating.backend.entity.UserProfileImage;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class UserProfileImageResponse {

    private Long id;
    private String imageUrl;
    private Integer imageOrder;
    private Boolean isMain;

    public static UserProfileImageResponse from(UserProfileImage image) {
        return new UserProfileImageResponse(
                image.getId(),
                image.getImageUrl(),
                image.getImageOrder(),
                image.getIsMain()
        );
    }
}
