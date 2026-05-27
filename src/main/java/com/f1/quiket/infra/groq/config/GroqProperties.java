package com.f1.quiket.infra.groq.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

@Getter
@Setter
@ConfigurationProperties(prefix = "ai.groq")
public class GroqProperties {

    private String baseUrl = "https://api.groq.com";
    private String apiKey;
    private String model = "llama-3.3-70b-versatile";
    private Double temperature = 0.2;
    private Integer connectTimeoutSeconds = 10;
    private Integer readTimeoutSeconds = 120;

    public boolean isConfigured() {
        return StringUtils.hasText(baseUrl)
                && StringUtils.hasText(apiKey)
                && StringUtils.hasText(model);
    }
}

