/**
 * ProfileImageService 비즈니스 로직
 */
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

    @Transactional(readOnly = true)
    public List<UserProfileImageResponse> getMyImages(String email) {
        User user = getUserByEmail(email);
        return userProfileImageRepository.findByUserIdOrderByImageOrderAsc(user.getId())
                .stream()
                .map(UserProfileImageResponse::from)
                .toList();
    }

    @Transactional
    public List<UserProfileImageResponse> saveMyImage(String email, UserProfileImageRequest request) {
        User user = getUserByEmail(email);
        validateImageOrder(request.getImageOrder());

        if (userProfileImageRepository.countByUserId(user.getId()) >= 5
                && userProfileImageRepository.findByUserIdAndImageOrder(user.getId(), request.getImageOrder()).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "프로필 사진은 최대 5장까지 등록할 수 있습니다.");
        }

        if (Boolean.TRUE.equals(request.getIsMain())) {
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
        image.setIsMain(Boolean.TRUE.equals(request.getIsMain()));
        userProfileImageRepository.saveAndFlush(image);

        ensureMainImage(user.getId());
        refreshReviewProgress(user);
        return getMyImages(email);
    }

    @Transactional(readOnly = true)
    public ReviewStatusResponse getReviewStatus(String email) {
        User user = getUserByEmail(email);
        UserProfile profile = userProfileRepository.findByUserId(user.getId()).orElse(null);
        UserVerification verification = userVerificationRepository.findByUserId(user.getId()).orElse(null);

        int imageCount = (int) userProfileImageRepository.countByUserId(user.getId());
        boolean readyForReview = imageCount >= 2;
        boolean profileComplete = profile != null
                && verification != null
                && accountReviewPolicyService.isProfileComplete(profile, verification);
        long remainingDays = accountReviewPolicyService.calculateRemainingDays(user.getReviewDeadlineAt());
        String deadlineWarning = accountReviewPolicyService.buildDeadlineWarning(user.getReviewDeadlineAt());

        String message = switch (user.getStatus()) {
            case "PENDING_REVIEW" -> buildPendingMessage(profileComplete, readyForReview, deadlineWarning);
            case "REJECTED" -> buildRejectedMessage(profileComplete, readyForReview, deadlineWarning);
            case "ACTIVE" -> "회원 가입 심사가 승인되었습니다. 메인 화면을 이용할 수 있습니다.";
            case "DELETED" -> "탈퇴 처리된 계정입니다. 새로운 계정으로 가입해 주세요.";
            default -> "심사 상태를 확인할 수 없습니다. 고객센터에 문의해 주세요.";
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
                        ? "프로필은 완료되었지만 사진이 아직 부족합니다. 대표 사진 포함 최소 2장을 등록해 주세요."
                        : "사진은 등록되었지만 프로필 필수 항목이 아직 부족합니다. 자기소개, 성격, 이상형, 직업, MBTI를 모두 입력해 주세요.");
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
            return appendDeadline("사진은 등록되었지만 프로필 필수 항목이 아직 부족합니다. 자기소개, 성격, 이상형, 직업, MBTI를 모두 입력해 주세요.", deadlineWarning);
        }
        return appendDeadline("프로필은 완료되었지만 사진이 아직 부족합니다. 대표 사진 포함 최소 2장을 등록해 주세요.", deadlineWarning);
    }

    private String buildRejectedMessage(boolean profileComplete, boolean readyForReview, String deadlineWarning) {
        if (profileComplete && readyForReview) {
            return appendDeadline("반려 사유를 확인하고 사진을 다시 등록했습니다. 운영자가 다시 검토할 때까지 기다려 주세요.", deadlineWarning);
        }
        return appendDeadline("반려 사유를 확인하고 프로필 또는 사진을 다시 보완해 주세요.", deadlineWarning);
    }

    private String appendDeadline(String baseMessage, String deadlineWarning) {
        if (deadlineWarning == null || deadlineWarning.isBlank()) {
            return baseMessage;
        }
        return baseMessage + " " + deadlineWarning;
    }

    private User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "유저를 찾을 수 없습니다."));
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

    private void validateImageOrder(Integer imageOrder) {
        if (imageOrder == null || imageOrder < 1 || imageOrder > 5) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "사진 순서는 1~5 사이여야 합니다.");
        }
    }
}
