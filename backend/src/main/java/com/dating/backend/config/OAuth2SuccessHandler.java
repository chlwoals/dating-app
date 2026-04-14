/**
 * OAuth2SuccessHandler 설정
 */
package com.dating.backend.config;

import com.dating.backend.entity.User;
import com.dating.backend.repository.UserRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Locale;

@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {
    private static final String PROVIDER_GOOGLE = "GOOGLE";
    private static final String PROVIDER_BOTH = "BOTH";

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

        User user = userRepository.findByEmail(email)
                .map(existingUser -> {
                    if (existingUser.getNickname() == null || existingUser.getNickname().isBlank()) {
                        existingUser.setNickname(name);
                    }
                    existingUser.setProvider(linkWithGoogleProvider(existingUser.getProvider()));
                    return userRepository.save(existingUser);
                })
                .orElseGet(() -> userRepository.save(
                        User.builder()
                                .email(email)
                                .nickname(name)
                                .provider(PROVIDER_GOOGLE)
                                .build()
                ));

        String token = jwtUtil.createToken(user.getEmail());
        response.sendRedirect("http://localhost:5173/social-login?token=" + token);
    }

    private String linkWithGoogleProvider(String provider) {
        String normalized = provider == null ? "" : provider.toUpperCase(Locale.ROOT);
        if (PROVIDER_GOOGLE.equals(normalized) || PROVIDER_BOTH.equals(normalized)) {
            return normalized.isBlank() ? PROVIDER_GOOGLE : normalized;
        }
        return PROVIDER_BOTH;
    }
}