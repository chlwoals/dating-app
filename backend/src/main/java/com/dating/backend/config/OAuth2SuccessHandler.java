package com.dating.backend.config;

import com.dating.backend.entity.User;
import com.dating.backend.repository.UserRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException, ServletException {

        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();

        String email = oAuth2User.getAttribute("email");
        String name = oAuth2User.getAttribute("name");

        // 🔥 DB 조회 or 자동 회원가입
        User user = userRepository.findByEmail(email)
                .orElseGet(() -> userRepository.save(
                        User.builder()
                                .email(email)
                                .nickname(name)
                                .provider("google")
                                .build()
                ));

        // 🔐 JWT 생성
        String token = jwtUtil.createToken(user.getEmail());

        // 🔥 프론트로 리다이렉트 (토큰 포함)
        response.sendRedirect("http://localhost:5173/social-login?token=" + token);
    }
}