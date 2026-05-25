package com.f1.quiket.infra.aoai.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

@Getter
@Setter
@ConfigurationProperties(prefix = "azure.openai")
public class AzureOpenAiProperties {

    private String endpoint;
    private String apiKey;
    private String deploymentName;
    private String apiVersion = "2024-10-21";
    private Integer maxOutputTokens = 4096;
    private Double temperature = 0.2;

    public boolean isConfigured() {
        return StringUtils.hasText(endpoint)
                && StringUtils.hasText(apiKey)
                && StringUtils.hasText(deploymentName)
                && StringUtils.hasText(apiVersion);
    }
}
