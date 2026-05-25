package com.f1.quiket.infra.aoai.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(AzureOpenAiProperties.class)
public class AzureOpenAiConfig {

    @Bean
    public RestClient azureOpenAiRestClient() {
        return RestClient.builder().build();
    }
}
