package com.f1.quiket.infra.mail.config;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * SES 설정값 바인딩
 */
@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "aws.ses")
public class AwsSesProperties {

    /**
     * SES 리전
     */
    private String region = "ap-northeast-2";

    /**
     * 기본 발신자 이메일
     */
    @NotBlank(message = "aws.ses.from-email 값 필수")
    private String fromEmail;
}
