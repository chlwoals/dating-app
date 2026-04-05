package com.dating.backend.controller;

import com.dating.backend.dto.ReviewStatusResponse;
import com.dating.backend.dto.UserProfileImageRequest;
import com.dating.backend.dto.UserProfileImageResponse;
import com.dating.backend.service.FileStorageService;
import com.dating.backend.service.ProfileImageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/profile-images")
@RequiredArgsConstructor
public class ProfileImageController {

    private final ProfileImageService profileImageService;
    private final FileStorageService fileStorageService;

    // 심사용 프로필 사진 목록을 조회한다.
    @GetMapping("/me")
    public List<UserProfileImageResponse> getMyImages(Authentication authentication) {
        return profileImageService.getMyImages(authentication.getName());
    }

    // 심사용 프로필 사진을 등록한다.
    @PostMapping("/me")
    public List<UserProfileImageResponse> saveMyImage(
            Authentication authentication,
            @Valid @RequestBody UserProfileImageRequest request
    ) {
        return profileImageService.saveMyImage(authentication.getName(), request);
    }

    // 실제 이미지 파일을 업로드하고 저장된 경로를 심사용 사진으로 등록한다.
    @PostMapping("/me/upload")
    public List<UserProfileImageResponse> uploadMyImage(
            Authentication authentication,
            @RequestParam("file") MultipartFile file,
            @RequestParam("imageOrder") Integer imageOrder,
            @RequestParam("isMain") Boolean isMain
    ) {
        String imageUrl = fileStorageService.storeProfileImage(
                profileImageService.getUserId(authentication.getName()),
                file
        );

        UserProfileImageRequest request = new UserProfileImageRequest();
        request.setImageUrl(imageUrl);
        request.setImageOrder(imageOrder);
        request.setIsMain(isMain);
        return profileImageService.saveMyImage(authentication.getName(), request);
    }

    // 내 계정의 사진 심사 상태를 조회한다.
    @GetMapping("/review-status")
    public ReviewStatusResponse getReviewStatus(Authentication authentication) {
        return profileImageService.getReviewStatus(authentication.getName());
    }
}
