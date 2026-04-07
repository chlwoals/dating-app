package com.dating.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "email", nullable = false, unique = true, length = 255)
    private String email;

    @Column(name = "nickname", nullable = false, unique = true, length = 255)
    private String nickname;

    @Column(name = "provider", nullable = false, length = 20)
    private String provider;

    @Column(name = "status", nullable = false, length = 30)
    @Builder.Default
    private String status = "ACTIVE";

    @Column(name = "review_comment", length = 255)
    private String reviewComment;

    @Column(name = "admin_memo", columnDefinition = "TEXT")
    private String adminMemo;

    @Column(name = "fraud_risk_score", nullable = false)
    @Builder.Default
    private Integer fraudRiskScore = 0;

    @Column(name = "fraud_review_status", nullable = false, length = 20)
    @Builder.Default
    private String fraudReviewStatus = "NORMAL";

    // 비밀번호는 해시된 값만 저장하며 users.password_hash 컬럼에 매핑한다.
    @Column(name = "password_hash", length = 255)
    private String password;

    @Column(name = "reset_password_token", length = 255)
    private String resetPasswordToken;

    @Column(name = "reset_password_token_expires_at")
    private LocalDateTime resetPasswordTokenExpiresAt;

    // 심사 대기 또는 반려 후 재등록 기한을 관리한다.
    @Column(name = "review_deadline_at")
    private LocalDateTime reviewDeadlineAt;

    @Column(name = "last_warning_sent_at")
    private LocalDateTime lastWarningSentAt;

    @Column(name = "profile_completed_at")
    private LocalDateTime profileCompletedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();
}