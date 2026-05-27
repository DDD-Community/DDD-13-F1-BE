package com.f1.quiket.infra.groq.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

/**
 * Groq API 설정값
 *
 * 호출 URL, 인증키, 모델, 타임아웃 관리
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "groq.openai")
public class GroqProperties {

    private String baseUrl = "https://api.groq.com";
    private String completionsPath = "/openai/v1/chat/completions";
    private String apiKey;
    private String model = "llama-4-scout-17b-16e-instruct";
    private Double temperature = 0.2;
    private Integer connectTimeoutSeconds = 10;
    private Integer readTimeoutSeconds = 120;

    /**
     * Groq 필수 설정값 존재 여부
     */
    public boolean isConfigured() {
        return StringUtils.hasText(baseUrl)
                && StringUtils.hasText(completionsPath)
                && StringUtils.hasText(apiKey)
                && StringUtils.hasText(model);
    }
}
