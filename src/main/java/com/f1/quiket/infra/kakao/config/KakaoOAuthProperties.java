package com.f1.quiket.infra.kakao.config;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Kakao OAuth 설정값 바인딩
 */
@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "kakao.oauth")
public class KakaoOAuthProperties {

    @NotBlank(message = "kakao.oauth.user-info-uri 값 필수")
    private String userInfoUri;
}
