package com.dating.backend.service;

import com.dating.backend.dto.AdminRiskActionRequest;
import com.dating.backend.dto.AdminUserReportResponse;
import com.dating.backend.dto.FraudRiskLogResponse;
import com.dating.backend.dto.MessageResponse;
import com.dating.backend.dto.ResolveReportRequest;
import com.dating.backend.dto.ScamMonitorSummaryResponse;
import com.dating.backend.dto.ScamMonitorUserResponse;
import com.dating.backend.dto.UserReportRequest;
import com.dating.backend.dto.UserReportResponse;
import com.dating.backend.entity.FraudRiskLog;
import com.dating.backend.entity.User;
import com.dating.backend.entity.UserProfile;
import com.dating.backend.entity.UserReport;
import com.dating.backend.repository.FraudRiskLogRepository;
import com.dating.backend.repository.UserProfileRepository;
import com.dating.backend.repository.UserReportRepository;
import com.dating.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class SafetyService {

    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final UserReportRepository userReportRepository;
    private final FraudRiskLogRepository fraudRiskLogRepository;
    private final BlockedIdentityService blockedIdentityService;

    @Value("${app.admin.review-key:change-admin-review-key}")
    private String adminReviewKey;

    @Transactional
    public UserReportResponse reportUser(String reporterEmail, UserReportRequest request) {
        User reporter = getUserByEmail(reporterEmail);
        User reportedUser = userRepository.findById(request.getReportedUserId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "?좉퀬 ????ъ슜?먮? 李얠쓣 ???놁뒿?덈떎."));

        if (reporter.getId().equals(reportedUser.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "蹂몄씤 怨꾩젙? ?좉퀬?????놁뒿?덈떎.");
        }

        if (userReportRepository.existsByReporterUserIdAndReportedUserIdAndStatus(reporter.getId(), reportedUser.getId(), "OPEN")) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "?대? 泥섎━ 以묒씤 ?좉퀬媛 ?덉뒿?덈떎.");
        }

        UserReport saved = userReportRepository.save(UserReport.builder()
                .reporterUserId(reporter.getId())
                .reportedUserId(reportedUser.getId())
                .reasonCode(request.getReasonCode())
                .detail(request.getDetail())
                .build());

        return new UserReportResponse(
                saved.getId(),
                saved.getReporterUserId(),
                saved.getReportedUserId(),
                saved.getReasonCode(),
                saved.getDetail(),
                saved.getStatus(),
                saved.getCreatedAt()
        );
    }

    public void validateAdminKey(String adminKey) {
        if (adminKey == null || !adminReviewKey.equals(adminKey)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "?댁쁺???몄쬆 ?ㅺ? ?щ컮瑜댁? ?딆뒿?덈떎.");
        }
    }

    @Transactional(readOnly = true)
    public ScamMonitorSummaryResponse getSummary() {
        return new ScamMonitorSummaryResponse(
                userRepository.countByFraudReviewStatus("HIGH_RISK"),
                userRepository.countByFraudReviewStatus("WATCH"),
                userReportRepository.countByStatus("OPEN"),
                userReportRepository.countByStatus("RESOLVED")
        );
    }

    @Transactional(readOnly = true)
    public List<ScamMonitorUserResponse> getRiskUsers(String riskStatus, String q) {
        String normalizedRiskStatus = (riskStatus == null || riskStatus.isBlank()) ? "WATCH" : riskStatus;
        String query = q == null ? "" : q.trim().toLowerCase(Locale.ROOT);

        return userRepository.findByFraudReviewStatusInOrderByFraudRiskScoreDescCreatedAtDesc(
                        "ALL".equalsIgnoreCase(normalizedRiskStatus) ? List.of("WATCH", "HIGH_RISK") : List.of(normalizedRiskStatus)
                ).stream()
                .map(user -> {
                    UserProfile profile = userProfileRepository.findByUserId(user.getId()).orElse(null);
                    String latestRiskDetail = fraudRiskLogRepository.findTop20ByUserIdOrderByCreatedAtDesc(user.getId()).stream()
                            .findFirst()
                            .map(FraudRiskLog::getDetail)
                            .orElse("理쒓렐 ?꾪뿕 濡쒓렇媛 ?놁뒿?덈떎.");

                    return new ScamMonitorUserResponse(
                            user.getId(),
                            user.getEmail(),
                            user.getNickname(),
                            user.getProvider(),
                            user.getStatus(),
                            user.getFraudRiskScore(),
                            user.getFraudReviewStatus(),
                            user.getAdminMemo(),
                            blockedIdentityService.hasActiveBlockedIdentity(user),
                            blockedIdentityService.summarizeBlockedTypes(user),
                            profile != null ? profile.getRegion() : null,
                            profile != null ? profile.getJob() : null,
                            userReportRepository.countByReportedUserIdAndStatus(user.getId(), "OPEN"),
                            latestRiskDetail,
                            user.getCreatedAt()
                    );
                })
                .filter(user -> query.isBlank()
                        || contains(user.getEmail(), query)
                        || contains(user.getNickname(), query)
                        || contains(user.getRegion(), query)
                        || contains(user.getJob(), query)
                        || contains(user.getBlockedIdentityTypes(), query)
                        || contains(user.getAdminMemo(), query))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<FraudRiskLogResponse> getRiskLogs(Long userId) {
        return fraudRiskLogRepository.findTop20ByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(log -> new FraudRiskLogResponse(
                        log.getId(),
                        log.getRiskType(),
                        log.getScore(),
                        log.getDetail(),
                        log.getCreatedAt()
                ))
                .toList();
    }

    @Transactional
    public MessageResponse reviewRiskUser(Long userId, AdminRiskActionRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "?ъ슜?먮? 李얠쓣 ???놁뒿?덈떎."));

        String action = request.getAction().toUpperCase(Locale.ROOT);
        String note = request.getAdminNote();

        switch (action) {
            case "MARK_NORMAL" -> {
                user.setFraudReviewStatus("NORMAL");
                user.setFraudRiskScore(0);
                if ("SUSPENDED".equals(user.getStatus())) {
                    user.setStatus("ACTIVE");
                }
                user.setReviewComment("운영 검토 중입니다.");
            }
            case "MARK_WATCH" -> {
                user.setFraudReviewStatus("WATCH");
                if (user.getFraudRiskScore() < 30) {
                    user.setFraudRiskScore(30);
                }
                if ("SUSPENDED".equals(user.getStatus())) {
                    user.setStatus("ACTIVE");
                }
                user.setReviewComment("운영 검토 중입니다.");
            }
            case "SUSPEND_ACCOUNT" -> {
                user.setStatus("SUSPENDED");
                if ("NORMAL".equals(user.getFraudReviewStatus())) {
                    user.setFraudReviewStatus("WATCH");
                }
                user.setReviewComment("운영 정책상 이용이 제한되었습니다.");
            }
            case "RELEASE_ACCOUNT" -> {
                user.setStatus("ACTIVE");
                if ("HIGH_RISK".equals(user.getFraudReviewStatus())) {
                    user.setFraudReviewStatus("WATCH");
                }
                user.setReviewComment("운영 검토 중입니다.");
            }
            case "BLOCK_IDENTITY" -> {
                user.setStatus("SUSPENDED");
                user.setFraudReviewStatus("HIGH_RISK");
                user.setReviewComment("운영 정책상 이용이 제한되었습니다.");
                blockedIdentityService.blockUserIdentities(user, note);
            }
            case "UNBLOCK_IDENTITY" -> {
                blockedIdentityService.unblockUserIdentities(user);
                if ("SUSPENDED".equals(user.getStatus())) {
                    user.setStatus("ACTIVE");
                }
                if ("HIGH_RISK".equals(user.getFraudReviewStatus())) {
                    user.setFraudReviewStatus("WATCH");
                }
                user.setReviewComment("운영 검토 중입니다.");
            }
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "지원하지 않는 처리 액션입니다.");
        }

        user.setAdminMemo(note);
        userRepository.save(user);
        fraudRiskLogRepository.save(FraudRiskLog.builder()
                .userId(user.getId())
                .riskType("ADMIN_REVIEW")
                .score(user.getFraudRiskScore())
                .detail(note)
                .build());

        return new MessageResponse("위험 계정 검토 결과를 반영했습니다.");
    }

    @Transactional(readOnly = true)
    public List<AdminUserReportResponse> getReports(String status) {
        String normalizedStatus = (status == null || status.isBlank()) ? "OPEN" : status;
        return userReportRepository.findByStatusOrderByCreatedAtDesc(normalizedStatus).stream()
                .map(report -> new AdminUserReportResponse(
                        report.getId(),
                        report.getReporterUserId(),
                        report.getReportedUserId(),
                        report.getReasonCode(),
                        report.getDetail(),
                        report.getStatus(),
                        report.getAdminNote(),
                        report.getCreatedAt()
                ))
                .toList();
    }

    @Transactional
    public MessageResponse resolveReport(Long reportId, ResolveReportRequest request) {
        UserReport report = userReportRepository.findById(reportId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "?좉퀬 ?대젰??李얠쓣 ???놁뒿?덈떎."));
        report.setStatus("RESOLVED");
        report.setAdminNote(request.getAdminNote());
        userReportRepository.save(report);
        return new MessageResponse("?좉퀬 ?대젰??泥섎━ ?꾨즺濡?蹂寃쎈릺?덉뒿?덈떎.");
    }

    private User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "?ъ슜?먮? 李얠쓣 ???놁뒿?덈떎."));
    }

    private boolean contains(String value, String query) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(query);
    }
}


