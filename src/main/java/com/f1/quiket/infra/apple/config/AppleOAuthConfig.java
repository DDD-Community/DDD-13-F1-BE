package com.f1.quiket.infra.apple.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/**
 * Apple OAuth 클라이언트 설정
 */
@Configuration
@EnableConfigurationProperties(AppleOAuthProperties.class)
public class AppleOAuthConfig {

    @Bean
    public RestClient appleRestClient() {
        return RestClient.builder().build();
    }
}
