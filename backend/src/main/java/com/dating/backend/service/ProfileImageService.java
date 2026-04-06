package com.dating.backend.service;

import com.dating.backend.dto.ReviewStatusResponse;
import com.dating.backend.dto.UserProfileImageRequest;
import com.dating.backend.dto.UserProfileImageResponse;
import com.dating.backend.entity.User;
import com.dating.backend.entity.UserProfile;
import com.dating.backend.entity.UserProfileImage;
import com.dating.backend.entity.UserVerification;
import com.dating.backend.repository.UserProfileImageRepository;
import com.dating.backend.repository.UserProfileRepository;
import com.dating.backend.repository.UserRepository;
import com.dating.backend.repository.UserVerificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProfileImageService {

    private final UserRepository userRepository;
    private final UserProfileImageRepository userProfileImageRepository;
    private final UserProfileRepository userProfileRepository;
    private final UserVerificationRepository userVerificationRepository;
    private final AccountReviewPolicyService accountReviewPolicyService;

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
            // 대표 사진을 바꿀 때는 기존 대표 표시를 먼저 DB에 반영해 제약 충돌을 막는다.
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
        refreshReviewProgress(user);
        return getMyImages(email);
    }

    // 심사 상태와 사진/프로필 진행 상황을 함께 반환한다.
    @Transactional(readOnly = true)
    public ReviewStatusResponse getReviewStatus(String email) {
        User user = getUserByEmail(email);
        UserProfile profile = userProfileRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "프로필 정보를 찾을 수 없습니다."));
        UserVerification verification = userVerificationRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "인증 정보를 찾을 수 없습니다."));

        int imageCount = (int) userProfileImageRepository.countByUserId(user.getId());
        boolean readyForReview = imageCount >= 2;
        boolean profileComplete = accountReviewPolicyService.isProfileComplete(profile, verification);
        long remainingDays = accountReviewPolicyService.calculateRemainingDays(user.getReviewDeadlineAt());
        String deadlineWarning = accountReviewPolicyService.buildDeadlineWarning(user.getReviewDeadlineAt());

        String message = switch (user.getStatus()) {
            case "PENDING_REVIEW" -> buildPendingMessage(profileComplete, readyForReview, deadlineWarning);
            case "REJECTED" -> buildRejectedMessage(profileComplete, readyForReview, deadlineWarning);
            case "ACTIVE" -> "가입 심사가 승인되었습니다. 이제 서비스를 이용할 수 있습니다.";
            case "DELETED" -> "기한 내에 필수 정보를 완료하지 않아 계정이 자동 정리되었습니다.";
            default -> "현재 계정 상태를 확인 중입니다.";
        };

        return new ReviewStatusResponse(
                user.getStatus(),
                imageCount,
                readyForReview,
                profileComplete,
                user.getReviewComment(),
                message,
                user.getReviewDeadlineAt(),
                remainingDays
        );
    }

    // 파일 업로드 서비스에서 사용할 사용자 ID를 조회한다.
    @Transactional(readOnly = true)
    public Long getUserId(String email) {
        return getUserByEmail(email).getId();
    }

    private void refreshReviewProgress(User user) {
        UserProfile profile = userProfileRepository.findByUserId(user.getId()).orElse(null);
        UserVerification verification = userVerificationRepository.findByUserId(user.getId()).orElse(null);
        if (profile == null || verification == null) {
            return;
        }

        boolean profileComplete = accountReviewPolicyService.isProfileComplete(profile, verification);
        long imageCount = userProfileImageRepository.countByUserId(user.getId());

        if (profileComplete && imageCount >= 2) {
            if (user.getProfileCompletedAt() == null) {
                user.setProfileCompletedAt(LocalDateTime.now());
            }
            if ("PENDING_REVIEW".equals(user.getStatus())) {
                user.setReviewComment("프로필과 사진 등록이 완료되어 심사 대기 중입니다.");
            }
        } else {
            user.setProfileCompletedAt(null);
            if ("PENDING_REVIEW".equals(user.getStatus()) || "REJECTED".equals(user.getStatus())) {
                user.setReviewComment(profileComplete
                        ? "사진이 아직 부족합니다. 대표 사진 포함 최소 2장을 등록해주세요."
                        : "프로필 필수 항목과 사진 등록을 완료해야 심사가 가능합니다.");
            }
        }

        userRepository.save(user);
    }

    private String buildPendingMessage(boolean profileComplete, boolean readyForReview, String deadlineWarning) {
        if (profileComplete && readyForReview) {
            return appendDeadline("프로필과 사진 등록이 완료되었습니다. 운영자 확인 후 승인 여부가 결정됩니다.", deadlineWarning);
        }
        if (!profileComplete && !readyForReview) {
            return appendDeadline("프로필 필수 항목과 대표 사진 포함 최소 2장을 모두 완료해야 심사로 넘어갈 수 있습니다.", deadlineWarning);
        }
        if (!profileComplete) {
            return appendDeadline("사진은 등록되었지만 프로필 필수 항목이 아직 부족합니다. 자기소개, 성격, 이상형, 직업, MBTI를 모두 입력해주세요.", deadlineWarning);
        }
        return appendDeadline("프로필은 완료되었지만 사진이 아직 부족합니다. 대표 사진 포함 최소 2장을 등록해주세요.", deadlineWarning);
    }

    private String buildRejectedMessage(boolean profileComplete, boolean readyForReview, String deadlineWarning) {
        if (profileComplete && readyForReview) {
            return appendDeadline("반려 사유를 확인한 뒤 사진을 다시 등록했습니다. 운영자가 다시 검토할 때까지 기다려주세요.", deadlineWarning);
        }
        return appendDeadline("반려 사유를 확인하고 프로필 또는 사진을 다시 보완해주세요.", deadlineWarning);
    }

    private String appendDeadline(String baseMessage, String deadlineWarning) {
        if (deadlineWarning == null || deadlineWarning.isBlank()) {
            return baseMessage;
        }
        return baseMessage + " " + deadlineWarning;
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