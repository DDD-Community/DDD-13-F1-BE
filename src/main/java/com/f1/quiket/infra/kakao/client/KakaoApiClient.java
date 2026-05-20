package com.f1.quiket.infra.kakao.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.f1.quiket.global.error.CustomException;
import com.f1.quiket.global.response.ErrorCode;
import com.f1.quiket.infra.kakao.config.KakaoOAuthProperties;
import com.f1.quiket.infra.kakao.dto.KakaoUserInfo;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

/**
 * Kakao 사용자 정보 API 클라이언트
 */
@Component
@RequiredArgsConstructor
public class KakaoApiClient {

    private static final String AUTHORIZATION = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final RestClient kakaoRestClient;
    private final KakaoOAuthProperties kakaoOAuthProperties;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public KakaoUserInfo getUserInfo(String kakaoAccessToken) {
        try {
            String responseBody = kakaoRestClient.get()
                    .uri(kakaoOAuthProperties.getUserInfoUri())
                    .accept(MediaType.APPLICATION_JSON)
                    .header(AUTHORIZATION, BEARER_PREFIX + kakaoAccessToken)
                    .retrieve()
                    .body(String.class);

            return parseUserInfo(responseBody);
        } catch (RestClientResponseException e) {
            if (e.getStatusCode().is4xxClientError()) {
                throw new CustomException(ErrorCode.AUTH_OAUTH_INVALID_TOKEN);
            }
            throw new CustomException(ErrorCode.AUTH_OAUTH_USER_INFO_FAILED);
        } catch (RestClientException | IOException e) {
            throw new CustomException(ErrorCode.AUTH_OAUTH_USER_INFO_FAILED);
        }
    }

    private KakaoUserInfo parseUserInfo(String responseBody) throws IOException {
        JsonNode rootNode = objectMapper.readTree(responseBody);
        JsonNode idNode = rootNode.path("id");
        if (idNode.isMissingNode() || idNode.isNull() || !StringUtils.hasText(idNode.asText())) {
            throw new CustomException(ErrorCode.AUTH_OAUTH_USER_INFO_FAILED);
        }

        JsonNode kakaoAccountNode = rootNode.path("kakao_account");
        return new KakaoUserInfo(
                idNode.asText(),
                textOrNull(kakaoAccountNode.path("email")),
                booleanOrNull(kakaoAccountNode.path("is_email_valid")),
                booleanOrNull(kakaoAccountNode.path("is_email_verified")),
                resolveNickname(rootNode, kakaoAccountNode)
        );
    }

    private String resolveNickname(JsonNode rootNode, JsonNode kakaoAccountNode) {
        String profileNickname = textOrNull(kakaoAccountNode.path("profile").path("nickname"));
        if (StringUtils.hasText(profileNickname)) {
            return profileNickname;
        }
        return textOrNull(rootNode.path("properties").path("nickname"));
    }

    private String textOrNull(JsonNode node) {
        if (node.isMissingNode() || node.isNull()) {
            return null;
        }
        String value = node.asText();
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value;
    }

    private Boolean booleanOrNull(JsonNode node) {
        if (node.isMissingNode() || node.isNull()) {
            return null;
        }
        return node.asBoolean();
    }
}
