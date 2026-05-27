package com.f1.quiket.infra.gemini.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

/**
 * Gemini API 설정값
 *
 * 호출 URL, 인증키, 모델, 타임아웃 관리
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "google.gemini")
public class GeminiProperties {

    private String baseUrl = "https://generativelanguage.googleapis.com";
    private String generateContentPath = "/v1beta/models/{model}:generateContent";
    private String apiKey;
    private String model = "gemini-3.1-flash-lite";
    private Double temperature = 0.2;
    private Integer connectTimeoutSeconds = 10;
    private Integer readTimeoutSeconds = 120;
    private Integer retryMaxAttempts = 3;
    private Long retryBackoffMillis = 1_000L;

    /**
     * Gemini 필수 설정값 존재 여부
     */
    public boolean isConfigured() {
        return StringUtils.hasText(baseUrl)
                && StringUtils.hasText(generateContentPath)
                && StringUtils.hasText(apiKey)
                && StringUtils.hasText(model);
    }
}
