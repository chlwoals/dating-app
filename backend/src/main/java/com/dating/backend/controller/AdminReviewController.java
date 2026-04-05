package com.dating.backend.controller;

import com.dating.backend.dto.AdminRejectRequest;
import com.dating.backend.dto.AdminReviewCandidateResponse;
import com.dating.backend.dto.AdminReviewDecisionResponse;
import com.dating.backend.service.AdminReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@RestController
@RequestMapping("/api/admin/reviews")
@RequiredArgsConstructor
public class AdminReviewController {

    private final AdminReviewService adminReviewService;

    // 운영자가 심사 대기 또는 반려 목록을 조회할 때 호출한다.
    @GetMapping
    public List<AdminReviewCandidateResponse> getCandidates(
            @RequestHeader("X-Admin-Key") String adminKey,
            @RequestParam(defaultValue = "PENDING_REVIEW") String status
    ) {
        adminReviewService.validateAdminKey(adminKey);
        return adminReviewService.getCandidates(status);
    }

    // 운영자가 사진 심사를 승인할 때 호출한다.
    @PostMapping("/{userId}/approve")
    public AdminReviewDecisionResponse approve(
            @PathVariable Long userId,
            @RequestHeader("X-Admin-Key") String adminKey
    ) {
        adminReviewService.validateAdminKey(adminKey);
        return adminReviewService.approve(userId);
    }

    // 운영자가 사진 심사를 반려할 때 호출한다.
    @PostMapping("/{userId}/reject")
    public AdminReviewDecisionResponse reject(
            @PathVariable Long userId,
            @RequestHeader("X-Admin-Key") String adminKey,
            @Valid @RequestBody AdminRejectRequest request
    ) {
        adminReviewService.validateAdminKey(adminKey);
        return adminReviewService.reject(userId, request);
    }
}
