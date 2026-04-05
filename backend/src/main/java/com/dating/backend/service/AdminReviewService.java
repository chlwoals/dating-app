package com.dating.backend.service;

import com.dating.backend.dto.AdminReviewCandidateResponse;
import com.dating.backend.dto.AdminReviewDecisionResponse;
import com.dating.backend.dto.AdminRejectRequest;
import com.dating.backend.entity.User;
import com.dating.backend.entity.UserProfile;
import com.dating.backend.entity.UserVerification;
import com.dating.backend.repository.UserProfileRepository;
import com.dating.backend.repository.UserProfileImageRepository;
import com.dating.backend.repository.UserRepository;
import com.dating.backend.repository.UserVerificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminReviewService {

    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final UserVerificationRepository userVerificationRepository;
    private final UserProfileImageRepository userProfileImageRepository;

    @Value("${app.admin.review-key:change-admin-review-key}")
    private String adminReviewKey;

    // 관리자 승인/반려 API는 간단한 운영 키 헤더를 통과한 요청만 처리한다.
    public void validateAdminKey(String adminKey) {
        if (adminKey == null || !adminReviewKey.equals(adminKey)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "운영자 인증 키가 올바르지 않습니다.");
        }
    }

    // 운영자가 심사해야 할 회원 목록을 조회한다.
    @Transactional(readOnly = true)
    public List<AdminReviewCandidateResponse> getCandidates(String status) {
        String normalizedStatus = (status == null || status.isBlank()) ? "PENDING_REVIEW" : status;

        return userRepository.findByStatusOrderByCreatedAtAsc(normalizedStatus).stream()
                .map(user -> {
                    UserProfile profile = userProfileRepository.findByUserId(user.getId())
                            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "프로필 정보를 찾을 수 없습니다."));
                    UserVerification verification = userVerificationRepository.findByUserId(user.getId())
                            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "인증 정보를 찾을 수 없습니다."));

                    return new AdminReviewCandidateResponse(
                            user.getId(),
                            user.getEmail(),
                            user.getNickname(),
                            user.getStatus(),
                            user.getReviewComment(),
                            user.getCreatedAt(),
                            verification.getBirthDate(),
                            verification.getGender(),
                            profile.getRegion(),
                            profile.getJob(),
                            profile.getMbti(),
                            profile.getIntroduction(),
                            userProfileImageRepository.findByUserIdOrderByImageOrderAsc(user.getId())
                                    .stream()
                                    .map(com.dating.backend.dto.UserProfileImageResponse::from)
                                    .toList()
                    );
                })
                .toList();
    }

    // 심사 요건을 만족한 계정을 승인 상태로 바꾼다.
    @Transactional
    public AdminReviewDecisionResponse approve(Long userId) {
        User user = getUser(userId);
        long imageCount = userProfileImageRepository.countByUserId(userId);

        if (imageCount < 2) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "승인하려면 최소 2장의 프로필 사진이 필요합니다.");
        }

        user.setStatus("ACTIVE");
        user.setReviewComment("프로필 사진 심사가 승인되었습니다.");
        userRepository.save(user);

        return new AdminReviewDecisionResponse(
                user.getId(),
                user.getStatus(),
                user.getReviewComment(),
                "회원 가입 심사가 승인되었습니다."
        );
    }

    // 심사 기준을 만족하지 못한 계정을 반려 상태로 바꾼다.
    @Transactional
    public AdminReviewDecisionResponse reject(Long userId, AdminRejectRequest request) {
        User user = getUser(userId);
        user.setStatus("REJECTED");
        user.setReviewComment(request.getReviewComment());
        userRepository.save(user);

        return new AdminReviewDecisionResponse(
                user.getId(),
                user.getStatus(),
                user.getReviewComment(),
                "회원 가입 심사가 반려되었습니다."
        );
    }

    private User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."));
    }
}
