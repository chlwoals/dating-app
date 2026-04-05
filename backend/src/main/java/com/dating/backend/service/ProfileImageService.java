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

    // 심사 제출용 프로필 사진 목록을 반환한다.
    @Transactional(readOnly = true)
    public List<UserProfileImageResponse> getMyImages(String email) {
        User user = getUserByEmail(email);
        return userProfileImageRepository.findByUserIdOrderByImageOrderAsc(user.getId())
                .stream()
                .map(UserProfileImageResponse::from)
                .toList();
    }

    // 가입 심사에 사용할 프로필 사진 URL을 등록하거나 같은 순서의 사진을 교체한다.
    @Transactional
    public List<UserProfileImageResponse> saveMyImage(String email, UserProfileImageRequest request) {
        User user = getUserByEmail(email);

        if (userProfileImageRepository.countByUserId(user.getId()) >= 5
                && userProfileImageRepository.findByUserIdAndImageOrder(user.getId(), request.getImageOrder()).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "프로필 사진은 최대 5장까지만 등록할 수 있습니다.");
        }

        if (request.getIsMain()) {
            userProfileImageRepository.findByUserIdOrderByImageOrderAsc(user.getId())
                    .forEach(image -> image.setIsMain(false));
        }

        UserProfileImage image = userProfileImageRepository.findByUserIdAndImageOrder(user.getId(), request.getImageOrder())
                .orElseGet(() -> UserProfileImage.builder()
                        .userId(user.getId())
                        .imageOrder(request.getImageOrder())
                        .build());

        image.setImageUrl(request.getImageUrl());
        image.setIsMain(request.getIsMain());
        userProfileImageRepository.save(image);

        ensureMainImage(user.getId());
        return getMyImages(email);
    }

    // 심사 대기/승인/반려 상태와 사진 등록 현황을 함께 내려준다.
    @Transactional(readOnly = true)
    public ReviewStatusResponse getReviewStatus(String email) {
        User user = getUserByEmail(email);
        int imageCount = (int) userProfileImageRepository.countByUserId(user.getId());
        boolean readyForReview = imageCount >= 2;

        String message = switch (user.getStatus()) {
            case "PENDING_REVIEW" -> readyForReview
                    ? "사진 제출이 완료되었습니다. 운영 검토 후 승인 여부가 결정됩니다."
                    : "가입 심사를 위해 대표 사진 포함 최소 2장의 프로필 사진을 등록해주세요.";
            case "REJECTED" -> "사진 심사 결과 반려되었습니다. 안내 문구를 확인하고 사진을 다시 등록해주세요.";
            case "ACTIVE" -> "가입 승인이 완료되었습니다. 이제 서비스 이용이 가능합니다.";
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

    // 파일 업로드 저장 서비스가 사용자 식별용 ID를 가져갈 때 사용한다.
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
