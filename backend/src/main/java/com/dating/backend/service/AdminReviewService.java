package com.dating.backend.service;

import com.dating.backend.dto.AdminMemoRequest;
import com.dating.backend.dto.AdminReviewCandidateResponse;
import com.dating.backend.dto.AdminReviewDecisionResponse;
import com.dating.backend.dto.AdminReviewSummaryResponse;
import com.dating.backend.dto.AdminRejectRequest;
import com.dating.backend.dto.MessageResponse;
import com.dating.backend.dto.UserProfileImageResponse;
import com.dating.backend.entity.User;
import com.dating.backend.entity.UserProfile;
import com.dating.backend.entity.UserVerification;
import com.dating.backend.repository.UserProfileImageRepository;
import com.dating.backend.repository.UserProfileRepository;
import com.dating.backend.repository.UserRepository;
import com.dating.backend.repository.UserVerificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminReviewService {

    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final UserVerificationRepository userVerificationRepository;
    private final UserProfileImageRepository userProfileImageRepository;
    private final AccountReviewPolicyService accountReviewPolicyService;

    @Value("${app.admin.review-key:change-admin-review-key}")
    private String adminReviewKey;

    public void validateAdminKey(String adminKey) {
        if (adminKey == null || !adminReviewKey.equals(adminKey)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "운영자 인증 키가 올바르지 않습니다.");
        }
    }

    @Transactional(readOnly = true)
    public AdminReviewSummaryResponse getSummary() {
        LocalDateTime dueSoonThreshold = LocalDateTime.now().plusDays(1);
        long dueSoonCount = userRepository.countByStatusInAndReviewDeadlineAtBefore(
                List.of("PENDING_REVIEW", "REJECTED"),
                dueSoonThreshold
        );

        return new AdminReviewSummaryResponse(
                userRepository.countByStatus("PENDING_REVIEW"),
                userRepository.countByStatus("REJECTED"),
                userRepository.countByStatus("ACTIVE"),
                dueSoonCount
        );
    }

    // 필요하면 마감 임박 회원만 따로 필터링해 운영자가 빠르게 확인할 수 있게 한다.
    @Transactional(readOnly = true)
    public List<AdminReviewCandidateResponse> getCandidates(String status, boolean dueSoonOnly) {
        String normalizedStatus = (status == null || status.isBlank()) ? "PENDING_REVIEW" : status;
        LocalDateTime dueSoonThreshold = LocalDateTime.now().plusDays(1);

        return userRepository.findByStatusOrderByCreatedAtAsc(normalizedStatus).stream()
                .filter(user -> !dueSoonOnly || (user.getReviewDeadlineAt() != null && !user.getReviewDeadlineAt().isAfter(dueSoonThreshold)))
                .map(user -> {
                    UserProfile profile = userProfileRepository.findByUserId(user.getId())
                            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "프로필 정보를 찾을 수 없습니다."));
                    UserVerification verification = userVerificationRepository.findByUserId(user.getId())
                            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "인증 정보를 찾을 수 없습니다."));

                    boolean profileComplete = accountReviewPolicyService.isProfileComplete(profile, verification);

                    return new AdminReviewCandidateResponse(
                            user.getId(),
                            user.getEmail(),
                            user.getNickname(),
                            user.getStatus(),
                            user.getReviewComment(),
                            user.getAdminMemo(),
                            user.getCreatedAt(),
                            user.getReviewDeadlineAt(),
                            profileComplete,
                            accountReviewPolicyService.calculateRemainingDays(user.getReviewDeadlineAt()),
                            verification.getBirthDate(),
                            verification.getGender(),
                            profile.getRegion(),
                            profile.getJob(),
                            profile.getMbti(),
                            profile.getIntroduction(),
                            userProfileImageRepository.findByUserIdOrderByImageOrderAsc(user.getId())
                                    .stream()
                                    .map(UserProfileImageResponse::from)
                                    .toList()
                    );
                })
                .toList();
    }

    @Transactional
    public AdminReviewDecisionResponse approve(Long userId) {
        User user = getUser(userId);
        UserProfile profile = userProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "프로필 정보를 찾을 수 없습니다."));
        UserVerification verification = userVerificationRepository.findByUserId(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "인증 정보를 찾을 수 없습니다."));

        long imageCount = userProfileImageRepository.countByUserId(userId);
        boolean profileComplete = accountReviewPolicyService.isProfileComplete(profile, verification);

        if (imageCount < 2) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "승인하려면 최소 2장의 프로필 사진이 필요합니다.");
        }

        if (!profileComplete) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "프로필 필수 항목이 모두 입력되어야 승인할 수 있습니다.");
        }

        user.setStatus("ACTIVE");
        user.setReviewComment("프로필 사진 심사가 승인되었습니다.");
        user.setReviewDeadlineAt(null);
        user.setLastWarningSentAt(null);
        userRepository.save(user);

        return new AdminReviewDecisionResponse(
                user.getId(),
                user.getStatus(),
                user.getReviewComment(),
                "회원 가입 심사가 승인되었습니다."
        );
    }

    @Transactional
    public AdminReviewDecisionResponse reject(Long userId, AdminRejectRequest request) {
        User user = getUser(userId);
        user.setStatus("REJECTED");
        user.setReviewComment(request.getReviewComment());
        user.setReviewDeadlineAt(accountReviewPolicyService.createRejectedDeadline());
        userRepository.save(user);

        return new AdminReviewDecisionResponse(
                user.getId(),
                user.getStatus(),
                user.getReviewComment(),
                "회원 가입 심사가 반려되었습니다."
        );
    }

    @Transactional
    public MessageResponse updateMemo(Long userId, AdminMemoRequest request) {
        User user = getUser(userId);
        user.setAdminMemo(request.getAdminMemo());
        userRepository.save(user);
        return new MessageResponse("운영자 메모를 저장했습니다.");
    }

    private User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."));
    }
}