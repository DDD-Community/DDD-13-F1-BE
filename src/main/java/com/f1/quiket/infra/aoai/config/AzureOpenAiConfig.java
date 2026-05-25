package com.f1.quiket.infra.aoai.config;

import java.net.http.HttpClient;
import java.time.Duration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(AzureOpenAiProperties.class)
public class AzureOpenAiConfig {

    /**
     * AOAI 호출 전용 RestClient
     *
     * - connect/read 타임아웃 명시적 설정 — 응답 지연 시 EC2 스레드 hang 방지
     * - 타임아웃 값은 {@link AzureOpenAiProperties}에서 override 가능
     */
    @Bean
    public RestClient azureOpenAiRestClient(AzureOpenAiProperties properties) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(properties.getConnectTimeoutSeconds()))
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(Duration.ofSeconds(properties.getReadTimeoutSeconds()));
        return RestClient.builder()
                .requestFactory(requestFactory)
                .build();
    }
}
