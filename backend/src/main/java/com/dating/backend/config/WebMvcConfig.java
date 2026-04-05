package com.dating.backend.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final UploadProperties uploadProperties;

    // 업로드된 프로필 사진을 /uploads/** URL로 바로 조회할 수 있게 매핑한다.
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String configuredDir = StringUtils.hasText(uploadProperties.dir()) ? uploadProperties.dir() : "uploads";
        Path uploadPath = Paths.get(configuredDir).toAbsolutePath().normalize();
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(uploadPath.toUri().toString());
    }
}
