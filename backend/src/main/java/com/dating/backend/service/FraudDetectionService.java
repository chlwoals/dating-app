package com.dating.backend.service;

import com.dating.backend.entity.FraudRiskLog;
import com.dating.backend.entity.User;
import com.dating.backend.entity.UserProfile;
import com.dating.backend.repository.FraudRiskLogRepository;
import com.dating.backend.repository.UserProfileRepository;
import com.dating.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class FraudDetectionService {

    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final FraudRiskLogRepository fraudRiskLogRepository;

    // 프로필 문구에 포함된 위험 키워드를 기준으로 내부 위험 점수를 계산한다.
    @Transactional
    public void evaluateUserProfile(Long userId) {
        User user = userRepository.findById(userId).orElse(null);
        UserProfile profile = userProfileRepository.findByUserId(userId).orElse(null);

        if (user == null || profile == null) {
            return;
        }

        String text = String.join(" ",
                safe(profile.getJob()),
                safe(profile.getPersonality()),
                safe(profile.getIdealType()),
                safe(profile.getIntroduction())
        ).toLowerCase(Locale.ROOT);

        Map<String, Integer> rules = new LinkedHashMap<>();
        rules.put("코인", 35);
        rules.put("가상자산", 35);
        rules.put("투자", 30);
        rules.put("수익 보장", 35);
        rules.put("리딩방", 40);
        rules.put("해외 계좌", 30);
        rules.put("송금", 35);
        rules.put("입금", 30);
        rules.put("환전", 30);
        rules.put("텔레그램", 25);
        rules.put("오픈채팅", 20);
        rules.put("라인으로", 20);
        rules.put("카카오로", 20);

        List<String> matchedKeywords = new ArrayList<>();
        int score = 0;

        for (Map.Entry<String, Integer> entry : rules.entrySet()) {
            if (text.contains(entry.getKey().toLowerCase(Locale.ROOT))) {
                matchedKeywords.add(entry.getKey());
                score += entry.getValue();
            }
        }

        score = Math.min(score, 100);

        String nextStatus = score >= 60 ? "HIGH_RISK" : score >= 30 ? "WATCH" : "NORMAL";
        boolean changed = !nextStatus.equals(user.getFraudReviewStatus()) || !Integer.valueOf(score).equals(user.getFraudRiskScore());

        user.setFraudRiskScore(score);
        user.setFraudReviewStatus(nextStatus);

        if ("HIGH_RISK".equals(nextStatus) && "ACTIVE".equals(user.getStatus())) {
            user.setStatus("SUSPENDED");
            user.setReviewComment("운영 정책상 이용이 제한되었습니다.");
        }

        userRepository.save(user);

        if (changed && score >= 30) {
            String detail = matchedKeywords.isEmpty()
                    ? "위험 키워드 기반 점수가 감지되었습니다."
                    : "위험 키워드 감지: " + String.join(", ", matchedKeywords);

            fraudRiskLogRepository.save(FraudRiskLog.builder()
                    .userId(userId)
                    .riskType("PROFILE_KEYWORD")
                    .score(score)
                    .detail(detail)
                    .build());
        }
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}