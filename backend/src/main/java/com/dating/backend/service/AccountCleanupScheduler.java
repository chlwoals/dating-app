package com.dating.backend.service;

import com.dating.backend.entity.User;
import com.dating.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class AccountCleanupScheduler {

    private final UserRepository userRepository;

    // 심사 대기 또는 반려 상태가 기한을 넘기면 계정을 자동 정리한다.
    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void cleanupExpiredReviewAccounts() {
        List<User> expiredUsers = userRepository.findByStatusInAndReviewDeadlineAtBefore(
                List.of("PENDING_REVIEW", "REJECTED"),
                LocalDateTime.now()
        );

        if (expiredUsers.isEmpty()) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        expiredUsers.forEach(user -> {
            user.setStatus("DELETED");
            user.setDeletedAt(now);
            user.setReviewDeadlineAt(null);
            user.setReviewComment("기한 내에 사진 또는 프로필 정보를 완료하지 않아 계정이 자동 정리되었습니다.");
        });

        userRepository.saveAll(expiredUsers);
        log.info("자동 정리된 계정 수: {}", expiredUsers.size());
    }
}