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

    @Value("${app.admin.review-key:change-admin-review-key}")
    private String adminReviewKey;

    @Transactional
    public UserReportResponse reportUser(String reporterEmail, UserReportRequest request) {
        User reporter = getUserByEmail(reporterEmail);
        User reportedUser = userRepository.findById(request.getReportedUserId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "신고 대상 사용자를 찾을 수 없습니다."));

        if (reporter.getId().equals(reportedUser.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "본인 계정은 신고할 수 없습니다.");
        }

        if (userReportRepository.existsByReporterUserIdAndReportedUserIdAndStatus(reporter.getId(), reportedUser.getId(), "OPEN")) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 처리 중인 신고가 있습니다.");
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
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "운영자 인증 키가 올바르지 않습니다.");
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
                            .orElse("최근 위험 로그가 없습니다.");

                    return new ScamMonitorUserResponse(
                            user.getId(),
                            user.getEmail(),
                            user.getNickname(),
                            user.getProvider(),
                            user.getStatus(),
                            user.getFraudRiskScore(),
                            user.getFraudReviewStatus(),
                            user.getReviewComment(),
                            user.getAdminMemo(),
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
                        || contains(user.getReviewComment(), query)
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
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."));

        String action = request.getAction().toUpperCase(Locale.ROOT);
        String note = request.getAdminNote();

        switch (action) {
            case "MARK_NORMAL" -> {
                user.setFraudReviewStatus("NORMAL");
                user.setFraudRiskScore(0);
                if ("SUSPENDED".equals(user.getStatus())) {
                    user.setStatus("ACTIVE");
                }
                user.setReviewComment("운영 검토 후 정상 계정으로 분류되었습니다.");
            }
            case "MARK_WATCH" -> {
                user.setFraudReviewStatus("WATCH");
                if (user.getFraudRiskScore() < 30) {
                    user.setFraudRiskScore(30);
                }
                if ("SUSPENDED".equals(user.getStatus())) {
                    user.setStatus("ACTIVE");
                }
                user.setReviewComment("운영 검토 후 주의 계정으로 유지됩니다.");
            }
            case "SUSPEND_ACCOUNT" -> {
                user.setStatus("SUSPENDED");
                if ("NORMAL".equals(user.getFraudReviewStatus())) {
                    user.setFraudReviewStatus("WATCH");
                }
                user.setReviewComment("운영 검토로 인해 계정 이용이 제한되었습니다.");
            }
            case "RELEASE_ACCOUNT" -> {
                user.setStatus("ACTIVE");
                if ("HIGH_RISK".equals(user.getFraudReviewStatus())) {
                    user.setFraudReviewStatus("WATCH");
                }
                user.setReviewComment("운영 검토 후 계정 이용 제한이 해제되었습니다.");
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
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "신고 이력을 찾을 수 없습니다."));
        report.setStatus("RESOLVED");
        report.setAdminNote(request.getAdminNote());
        userReportRepository.save(report);
        return new MessageResponse("신고 이력이 처리 완료로 변경되었습니다.");
    }

    private User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."));
    }

    private boolean contains(String value, String query) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(query);
    }
}