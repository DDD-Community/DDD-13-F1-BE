package com.f1.quiket.infra.kakao.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/**
 * Kakao OAuth 클라이언트 설정
 */
@Configuration
@EnableConfigurationProperties(KakaoOAuthProperties.class)
public class KakaoOAuthConfig {

    @Bean
    public RestClient kakaoRestClient() {
        return RestClient.builder().build();
    }
}
