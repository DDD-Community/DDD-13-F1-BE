package com.f1.quiket.infra.apple.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.f1.quiket.global.error.CustomException;
import com.f1.quiket.global.response.ErrorCode;
import com.f1.quiket.infra.apple.config.AppleOAuthProperties;
import com.f1.quiket.infra.apple.dto.AppleUserInfo;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.RSAPublicKeySpec;
import java.time.Instant;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Apple identityToken(JWT) 검증기
 *
 * Apple JWKS 공개키로 RS256 서명 검증 후 iss/aud/exp 클레임 확인
 */
@Component
@Slf4j
public class AppleIdTokenVerifier {

    private static final String APPLE_ISSUER = "https://appleid.apple.com";
    private static final String ALG_RS256 = "RS256";
    private static final long JWKS_CACHE_TTL_MILLIS = 24L * 60 * 60 * 1000;
    private static final long JWKS_REFETCH_MIN_INTERVAL_MILLIS = 60L * 1000;

    private final RestClient appleRestClient;
    private final AppleOAuthProperties properties;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // 불변 맵 원자 교체 방식 - clear 후 재적재 사이의 빈 캐시 노출 방지
    private volatile Map<String, PublicKey> cachedKeys = Map.of();
    private final AtomicLong cachedAtMillis = new AtomicLong(0);

    public AppleIdTokenVerifier(
            @Qualifier("appleRestClient") RestClient appleRestClient,
            AppleOAuthProperties properties
    ) {
        this.appleRestClient = appleRestClient;
        this.properties = properties;
    }

    /**
     * identityToken 검증 후 사용자 정보 추출
     */
    public AppleUserInfo verify(String identityToken) {
        validateConfigured();

        String[] parts = splitToken(identityToken);
        JsonNode header = parseJson(decodeBase64Url(parts[0]));
        String kid = header.path("kid").asText(null);
        String alg = header.path("alg").asText(null);
        if (!StringUtils.hasText(kid) || !ALG_RS256.equals(alg)) {
            throw new CustomException(ErrorCode.AUTH_APPLE_INVALID_TOKEN);
        }

        verifySignature(parts, kid);

        JsonNode payload = parseJson(decodeBase64Url(parts[1]));
        validateClaims(payload);

        String subject = payload.path("sub").asText(null);
        if (!StringUtils.hasText(subject)) {
            throw new CustomException(ErrorCode.AUTH_APPLE_INVALID_TOKEN);
        }
        return new AppleUserInfo(
                subject,
                textOrNull(payload.path("email")),
                booleanOrNull(payload.path("email_verified"))
        );
    }

    private void validateConfigured() {
        if (!properties.isVerifierConfigured()) {
            log.warn("Apple OAuth client id is not configured");
            throw new CustomException(ErrorCode.AUTH_APPLE_CONFIG_ERROR);
        }
    }

    private String[] splitToken(String identityToken) {
        if (!StringUtils.hasText(identityToken)) {
            throw new CustomException(ErrorCode.AUTH_APPLE_INVALID_TOKEN);
        }
        String[] parts = identityToken.split("\\.");
        if (parts.length != 3) {
            throw new CustomException(ErrorCode.AUTH_APPLE_INVALID_TOKEN);
        }
        return parts;
    }

    private void verifySignature(String[] parts, String kid) {
        PublicKey publicKey = resolvePublicKey(kid);
        try {
            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initVerify(publicKey);
            signature.update((parts[0] + "." + parts[1]).getBytes(StandardCharsets.UTF_8));
            if (!signature.verify(Base64.getUrlDecoder().decode(parts[2]))) {
                throw new CustomException(ErrorCode.AUTH_APPLE_INVALID_TOKEN);
            }
        } catch (CustomException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Apple identity token signature verification failed", e);
            throw new CustomException(ErrorCode.AUTH_APPLE_INVALID_TOKEN);
        }
    }

    private void validateClaims(JsonNode payload) {
        if (!APPLE_ISSUER.equals(payload.path("iss").asText(null))) {
            throw new CustomException(ErrorCode.AUTH_APPLE_INVALID_TOKEN);
        }
        if (!containsAudience(payload.path("aud"), properties.getClientId())) {
            throw new CustomException(ErrorCode.AUTH_APPLE_INVALID_TOKEN);
        }
        long expEpochSeconds = payload.path("exp").asLong(0);
        if (expEpochSeconds <= Instant.now().getEpochSecond()) {
            throw new CustomException(ErrorCode.AUTH_APPLE_INVALID_TOKEN);
        }
    }

    private boolean containsAudience(JsonNode audNode, String clientId) {
        if (audNode.isArray()) {
            for (JsonNode value : audNode) {
                if (clientId.equals(value.asText(null))) {
                    return true;
                }
            }
            return false;
        }
        return clientId.equals(audNode.asText(null));
    }

    private PublicKey resolvePublicKey(String kid) {
        PublicKey cached = freshCachedKey(kid);
        if (cached != null) {
            return cached;
        }

        synchronized (this) {
            PublicKey refreshed = freshCachedKey(kid);
            if (refreshed != null) {
                return refreshed;
            }
            // 최소 재조회 간격 - 존재하지 않는 kid 반복 요청으로 인한 JWKS 호출 남발 방지
            if (System.currentTimeMillis() - cachedAtMillis.get() >= JWKS_REFETCH_MIN_INTERVAL_MILLIS) {
                fetchJwks();
            }
            PublicKey publicKey = cachedKeys.get(kid);
            if (publicKey == null) {
                log.warn("Apple JWKS does not contain kid={}", kid);
                throw new CustomException(ErrorCode.AUTH_APPLE_INVALID_TOKEN);
            }
            return publicKey;
        }
    }

    private PublicKey freshCachedKey(String kid) {
        boolean fresh = System.currentTimeMillis() - cachedAtMillis.get() < JWKS_CACHE_TTL_MILLIS;
        return fresh ? cachedKeys.get(kid) : null;
    }

    private void fetchJwks() {
        try {
            String responseBody = appleRestClient.get()
                    .uri(jwksUri())
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(String.class);
            if (!StringUtils.hasText(responseBody)) {
                throw new CustomException(ErrorCode.AUTH_APPLE_KEY_FETCH_FAILED);
            }

            JsonNode keys = objectMapper.readTree(responseBody).path("keys");
            if (!keys.isArray() || keys.isEmpty()) {
                throw new CustomException(ErrorCode.AUTH_APPLE_KEY_FETCH_FAILED);
            }

            Map<String, PublicKey> refreshedKeys = new HashMap<>();
            for (JsonNode key : keys) {
                String kid = key.path("kid").asText(null);
                String modulus = key.path("n").asText(null);
                String exponent = key.path("e").asText(null);
                if (StringUtils.hasText(kid) && StringUtils.hasText(modulus) && StringUtils.hasText(exponent)) {
                    refreshedKeys.put(kid, toRsaPublicKey(modulus, exponent));
                }
            }
            cachedKeys = Map.copyOf(refreshedKeys);
            cachedAtMillis.set(System.currentTimeMillis());
        } catch (CustomException e) {
            throw e;
        } catch (RestClientException | IOException e) {
            log.warn("Apple JWKS fetch failed", e);
            throw new CustomException(ErrorCode.AUTH_APPLE_KEY_FETCH_FAILED);
        } catch (Exception e) {
            log.warn("Apple JWKS parse failed", e);
            throw new CustomException(ErrorCode.AUTH_APPLE_KEY_FETCH_FAILED);
        }
    }

    private PublicKey toRsaPublicKey(String modulusBase64Url, String exponentBase64Url) throws Exception {
        BigInteger modulus = new BigInteger(1, Base64.getUrlDecoder().decode(modulusBase64Url));
        BigInteger exponent = new BigInteger(1, Base64.getUrlDecoder().decode(exponentBase64Url));
        return KeyFactory.getInstance("RSA").generatePublic(new RSAPublicKeySpec(modulus, exponent));
    }

    private String jwksUri() {
        String baseUrl = properties.getBaseUrl().replaceAll("/+$", "");
        return UriComponentsBuilder.fromUriString(baseUrl)
                .path(properties.getJwksPath())
                .build()
                .toUriString();
    }

    private JsonNode parseJson(byte[] json) {
        try {
            return objectMapper.readTree(json);
        } catch (IOException e) {
            throw new CustomException(ErrorCode.AUTH_APPLE_INVALID_TOKEN);
        }
    }

    private byte[] decodeBase64Url(String value) {
        try {
            return Base64.getUrlDecoder().decode(value);
        } catch (IllegalArgumentException e) {
            throw new CustomException(ErrorCode.AUTH_APPLE_INVALID_TOKEN);
        }
    }

    private String textOrNull(JsonNode node) {
        if (node.isMissingNode() || node.isNull()) {
            return null;
        }
        String value = node.asText();
        return StringUtils.hasText(value) ? value : null;
    }

    private Boolean booleanOrNull(JsonNode node) {
        if (node.isMissingNode() || node.isNull()) {
            return null;
        }
        // Apple email_verified는 boolean 또는 문자열 "true"/"false" 반환
        return node.asBoolean();
    }
}
