package com.f1.quiket.infra.groq.config;

import java.net.http.HttpClient;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * Groq 외부 연동 설정
 *
 * 전용 RestClient와 설정 properties 등록
 */
@Configuration
@EnableConfigurationProperties(GroqProperties.class)
public class GroqConfig {

    /**
     * Groq 호출 전용 RestClient
     */
    @Bean
    @Qualifier("groqRestClient")
    public RestClient groqRestClient(GroqProperties properties) {
        // Groq 연결 타임아웃 설정
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(properties.getConnectTimeoutSeconds()))
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        // Groq 응답 대기 타임아웃 설정
        requestFactory.setReadTimeout(Duration.ofSeconds(properties.getReadTimeoutSeconds()));
        return RestClient.builder().requestFactory(requestFactory).build();
    }
}
