package com.f1.quiket.infra.apple.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

/**
 * Apple OAuth 설정값 바인딩
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "apple.oauth")
public class AppleOAuthProperties {

    private String baseUrl = "https://appleid.apple.com";
    private String jwksPath = "/auth/keys";
    private String tokenPath = "/auth/token";
    private String revokePath = "/auth/revoke";

    /** iOS 앱 번들 ID - identityToken aud 검증값 */
    private String clientId = "";

    /** Apple Developer Team ID - client_secret iss */
    private String teamId = "";

    /** Sign in with Apple Key ID - client_secret kid */
    private String keyId = "";

    /** .p8 개인키 PEM 본문 - client_secret ES256 서명키 */
    private String privateKey = "";

    /** identityToken 검증 가능 여부 */
    public boolean isVerifierConfigured() {
        return StringUtils.hasText(clientId);
    }

    /** client_secret 발급 가능 여부 - token 교환/revoke 용 */
    public boolean isClientSecretConfigured() {
        return StringUtils.hasText(clientId)
                && StringUtils.hasText(teamId)
                && StringUtils.hasText(keyId)
                && StringUtils.hasText(privateKey);
    }
}
