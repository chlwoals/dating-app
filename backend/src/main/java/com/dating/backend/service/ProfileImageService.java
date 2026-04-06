package com.dating.backend.service;

import com.dating.backend.dto.ReviewStatusResponse;
import com.dating.backend.dto.UserProfileImageRequest;
import com.dating.backend.dto.UserProfileImageResponse;
import com.dating.backend.entity.User;
import com.dating.backend.entity.UserProfileImage;
import com.dating.backend.repository.UserProfileImageRepository;
import com.dating.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProfileImageService {

    private final UserRepository userRepository;
    private final UserProfileImageRepository userProfileImageRepository;

    // 사용자가 등록한 프로필 사진 목록을 반환한다.
    @Transactional(readOnly = true)
    public List<UserProfileImageResponse> getMyImages(String email) {
        User user = getUserByEmail(email);
        return userProfileImageRepository.findByUserIdOrderByImageOrderAsc(user.getId())
                .stream()
                .map(UserProfileImageResponse::from)
                .toList();
    }

    // 같은 순서의 사진이 있으면 교체하고, 없으면 새 사진으로 추가한다.
    @Transactional
    public List<UserProfileImageResponse> saveMyImage(String email, UserProfileImageRequest request) {
        User user = getUserByEmail(email);

        if (userProfileImageRepository.countByUserId(user.getId()) >= 5
                && userProfileImageRepository.findByUserIdAndImageOrder(user.getId(), request.getImageOrder()).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "프로필 사진은 최대 5장까지만 등록할 수 있습니다.");
        }

        if (request.getIsMain()) {
            // 대표 사진을 바꿀 때는 기존 대표 표시를 먼저 DB에 반영해
            // generated column unique 제약과 충돌하지 않도록 한다.
            List<UserProfileImage> existingImages = userProfileImageRepository.findByUserIdOrderByImageOrderAsc(user.getId());
            existingImages.forEach(image -> image.setIsMain(false));
            userProfileImageRepository.saveAllAndFlush(existingImages);
        }

        UserProfileImage image = userProfileImageRepository.findByUserIdAndImageOrder(user.getId(), request.getImageOrder())
                .orElseGet(() -> UserProfileImage.builder()
                        .userId(user.getId())
                        .imageOrder(request.getImageOrder())
                        .build());

        image.setImageUrl(request.getImageUrl());
        image.setIsMain(request.getIsMain());
        userProfileImageRepository.saveAndFlush(image);

        ensureMainImage(user.getId());
        return getMyImages(email);
    }

    // 심사 상태와 사진 등록 조건 충족 여부를 함께 반환한다.
    @Transactional(readOnly = true)
    public ReviewStatusResponse getReviewStatus(String email) {
        User user = getUserByEmail(email);
        int imageCount = (int) userProfileImageRepository.countByUserId(user.getId());
        boolean readyForReview = imageCount >= 2;

        String message = switch (user.getStatus()) {
            case "PENDING_REVIEW" -> readyForReview
                    ? "사진 등록이 완료되었습니다. 운영자 확인 후 승인 여부가 결정됩니다."
                    : "가입 심사를 위해 대표 사진 포함 최소 2장의 프로필 사진을 등록해주세요.";
            case "REJECTED" -> "사진 심사 결과 반려되었습니다. 안내 문구를 확인하고 사진을 다시 등록해주세요.";
            case "ACTIVE" -> "가입 승인이 완료되었습니다. 이제 서비스를 이용할 수 있습니다.";
            default -> "현재 계정 상태를 확인 중입니다.";
        };

        return new ReviewStatusResponse(
                user.getStatus(),
                imageCount,
                readyForReview,
                user.getReviewComment(),
                message
        );
    }

    // 파일 업로드 서비스에서 사용자 ID를 조회할 때 사용한다.
    @Transactional(readOnly = true)
    public Long getUserId(String email) {
        return getUserByEmail(email).getId();
    }

    private User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."));
    }

    private void ensureMainImage(Long userId) {
        List<UserProfileImage> images = userProfileImageRepository.findByUserIdOrderByImageOrderAsc(userId);
        if (images.isEmpty()) {
            return;
        }

        boolean hasMain = images.stream().anyMatch(UserProfileImage::getIsMain);
        if (!hasMain) {
            UserProfileImage firstImage = images.get(0);
            firstImage.setIsMain(true);
            userProfileImageRepository.save(firstImage);
        }
    }
}