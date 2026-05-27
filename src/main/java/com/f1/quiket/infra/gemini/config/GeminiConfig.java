package com.f1.quiket.infra.gemini.config;

import java.net.http.HttpClient;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * Gemini 외부 연동 설정
 *
 * 전용 RestClient와 설정 properties 등록
 */
@Configuration
@EnableConfigurationProperties(GeminiProperties.class)
public class GeminiConfig {

    /**
     * Gemini 호출 전용 RestClient
     */
    @Bean
    @Qualifier("geminiRestClient")
    public RestClient geminiRestClient(GeminiProperties properties) {
        // Gemini 연결 타임아웃 설정
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(properties.getConnectTimeoutSeconds()))
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        // Gemini 응답 대기 타임아웃 설정
        requestFactory.setReadTimeout(Duration.ofSeconds(properties.getReadTimeoutSeconds()));
        return RestClient.builder().requestFactory(requestFactory).build();
    }
}

