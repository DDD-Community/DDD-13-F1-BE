package com.f1.quiket.infra.mail.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sesv2.SesV2Client;

/**
 * SES 클라이언트 설정
 */
@Configuration
@EnableConfigurationProperties(AwsSesProperties.class)
public class AwsSesConfig {

    private static final String AWS_AUTH_SCHEME_PREFERENCE = "aws.authSchemePreference";
    private static final String AWS_AUTH_SCHEME_PREFERENCE_ENV = "AWS_AUTH_SCHEME_PREFERENCE";
    private static final String DEFAULT_AUTH_SCHEME = "sigv4";

    @Bean
    public SesV2Client sesV2Client(AwsSesProperties awsSesProperties) {
        // 자격증명 로딩 경로
        // AWS SDK 기본 Credentials Provider Chain
        // 리전 설정값 소스: aws.ses.region
        applyDefaultAuthSchemePreference();

        return SesV2Client.builder()
                .region(Region.of(awsSesProperties.getRegion()))
                .build();
    }

    private void applyDefaultAuthSchemePreference() {
        // 인증 스킴 기본값
        // 프로필 파싱 오류 회피
        boolean hasSystemValue = System.getProperty(AWS_AUTH_SCHEME_PREFERENCE) != null;
        boolean hasEnvValue = System.getenv(AWS_AUTH_SCHEME_PREFERENCE_ENV) != null;
        if (!hasSystemValue && !hasEnvValue) {
            System.setProperty(AWS_AUTH_SCHEME_PREFERENCE, DEFAULT_AUTH_SCHEME);
        }
    }
}
