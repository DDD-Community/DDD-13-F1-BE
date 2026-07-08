package com.f1.quiket.infra.apple.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.f1.quiket.infra.apple.config.AppleOAuthProperties;
import java.io.IOException;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Apple 토큰 API 클라이언트
 *
 * authorization code 교환(refresh token 확보) 및 탈퇴 시 토큰 revoke 처리
 * 두 호출 모두 best-effort - 실패 시 로그인/탈퇴 흐름을 막지 않음
 */
@Component
@Slf4j
public class AppleAuthApiClient {

    private final RestClient appleRestClient;
    private final AppleOAuthProperties properties;
    private final AppleClientSecretGenerator clientSecretGenerator;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AppleAuthApiClient(
            @Qualifier("appleRestClient") RestClient appleRestClient,
            AppleOAuthProperties properties,
            AppleClientSecretGenerator clientSecretGenerator
    ) {
        this.appleRestClient = appleRestClient;
        this.properties = properties;
        this.clientSecretGenerator = clientSecretGenerator;
    }

    /**
     * authorization code를 refresh token으로 교환
     *
     * 탈퇴 시 revoke에 사용할 refresh token 확보 목적, 실패 시 빈 값 반환
     */
    public Optional<String> exchangeAuthorizationCode(String authorizationCode) {
        if (!StringUtils.hasText(authorizationCode) || !properties.isClientSecretConfigured()) {
            return Optional.empty();
        }

        try {
            MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
            form.add("client_id", properties.getClientId());
            form.add("client_secret", clientSecretGenerator.generate());
            form.add("code", authorizationCode);
            form.add("grant_type", "authorization_code");

            String responseBody = appleRestClient.post()
                    .uri(uri(properties.getTokenPath()))
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(form)
                    .retrieve()
                    .body(String.class);
            if (!StringUtils.hasText(responseBody)) {
                log.warn("Apple token exchange returned empty body");
                return Optional.empty();
            }

            JsonNode root = objectMapper.readTree(responseBody);
            String refreshToken = root.path("refresh_token").asText(null);
            if (!StringUtils.hasText(refreshToken)) {
                log.warn("Apple token exchange response has no refresh_token");
                return Optional.empty();
            }
            return Optional.of(refreshToken);
        } catch (IOException | RuntimeException e) {
            log.warn("Apple token exchange failed", e);
            return Optional.empty();
        }
    }

    /**
     * refresh token revoke - 회원탈퇴 시 Apple 연동 해제
     */
    public boolean revoke(String refreshToken) {
        if (!StringUtils.hasText(refreshToken) || !properties.isClientSecretConfigured()) {
            return false;
        }

        try {
            MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
            form.add("client_id", properties.getClientId());
            form.add("client_secret", clientSecretGenerator.generate());
            form.add("token", refreshToken);
            form.add("token_type_hint", "refresh_token");

            appleRestClient.post()
                    .uri(uri(properties.getRevokePath()))
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .toBodilessEntity();
            return true;
        } catch (RuntimeException e) {
            log.warn("Apple token revoke failed", e);
            return false;
        }
    }

    private String uri(String path) {
        String baseUrl = properties.getBaseUrl().replaceAll("/+$", "");
        return UriComponentsBuilder.fromUriString(baseUrl)
                .path(path)
                .build()
                .toUriString();
    }
}
