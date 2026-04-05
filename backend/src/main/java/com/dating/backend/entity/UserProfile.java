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
@Table(name = "user_profiles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserProfile {

    // 소개팅 앱에서 노출되는 프로필 본문 정보를 저장한다.

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Long userId;

    @Column(nullable = false)
    private String region;

    @Column
    private String job;

    @Column(length = 4)
    private String mbti;

    @Column(columnDefinition = "TEXT")
    private String personality;

    @Column(columnDefinition = "TEXT")
    private String idealType;

    @Column(columnDefinition = "TEXT")
    private String introduction;

    @Column(nullable = false)
    @Builder.Default
    private String smokingStatus = "NON_SMOKER";

    @Column(nullable = false)
    @Builder.Default
    private String drinkingStatus = "NONE";

    @Column(nullable = false)
    @Builder.Default
    private String religion = "NONE";

    @Column(nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(nullable = false)
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();
}
