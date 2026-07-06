package com.f1.quiket.infra.apple.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.f1.quiket.global.error.CustomException;
import com.f1.quiket.global.response.ErrorCode;
import com.f1.quiket.infra.apple.config.AppleOAuthProperties;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Instant;
import java.util.Arrays;
import java.util.Base64;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Apple client_secret(ES256 JWT) 생성기
 *
 * Apple Developer 발급 .p8 개인키로 서명, /auth/token·/auth/revoke 호출 시 사용
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class AppleClientSecretGenerator {

    private static final String APPLE_AUDIENCE = "https://appleid.apple.com";
    private static final long CLIENT_SECRET_TTL_SECONDS = 300L;
    private static final int ES256_SIGNATURE_COMPONENT_LENGTH = 32;

    private final AppleOAuthProperties properties;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public String generate() {
        if (!properties.isClientSecretConfigured()) {
            log.warn("Apple client secret credentials are not configured");
            throw new CustomException(ErrorCode.AUTH_APPLE_CONFIG_ERROR);
        }

        try {
            long nowEpochSeconds = Instant.now().getEpochSecond();
            String header = encodeBase64Url(objectMapper.writeValueAsBytes(Map.of(
                    "alg", "ES256",
                    "kid", properties.getKeyId()
            )));
            String payload = encodeBase64Url(objectMapper.writeValueAsBytes(Map.of(
                    "iss", properties.getTeamId(),
                    "iat", nowEpochSeconds,
                    "exp", nowEpochSeconds + CLIENT_SECRET_TTL_SECONDS,
                    "aud", APPLE_AUDIENCE,
                    "sub", properties.getClientId()
            )));

            String unsignedToken = header + "." + payload;
            byte[] derSignature = signEs256(unsignedToken);
            return unsignedToken + "." + encodeBase64Url(derToJose(derSignature));
        } catch (CustomException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Apple client secret generation failed", e);
            throw new CustomException(ErrorCode.AUTH_APPLE_CONFIG_ERROR);
        }
    }

    private byte[] signEs256(String unsignedToken) throws Exception {
        Signature signature = Signature.getInstance("SHA256withECDSA");
        signature.initSign(parsePrivateKey());
        signature.update(unsignedToken.getBytes(StandardCharsets.UTF_8));
        return signature.sign();
    }

    private PrivateKey parsePrivateKey() throws Exception {
        String pem = properties.getPrivateKey()
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s", "");
        byte[] pkcs8 = Base64.getDecoder().decode(pem);
        return KeyFactory.getInstance("EC").generatePrivate(new PKCS8EncodedKeySpec(pkcs8));
    }

    /**
     * DER 인코딩 ECDSA 서명을 JOSE 형식(R||S 64바이트)으로 변환
     */
    private byte[] derToJose(byte[] derSignature) {
        // DER: SEQUENCE(0x30, len) { INTEGER(0x02, rLen, r), INTEGER(0x02, sLen, s) }
        int rLength = derSignature[3];
        byte[] r = Arrays.copyOfRange(derSignature, 4, 4 + rLength);
        int sOffset = 4 + rLength + 2;
        byte[] s = Arrays.copyOfRange(derSignature, sOffset, derSignature.length);

        byte[] jose = new byte[ES256_SIGNATURE_COMPONENT_LENGTH * 2];
        copyPadded(r, jose, 0);
        copyPadded(s, jose, ES256_SIGNATURE_COMPONENT_LENGTH);
        return jose;
    }

    private void copyPadded(byte[] component, byte[] target, int targetOffset) {
        // DER INTEGER 선행 0x00 패딩 제거 후 32바이트 좌측 0 패딩 정렬
        int sourceOffset = Math.max(0, component.length - ES256_SIGNATURE_COMPONENT_LENGTH);
        int length = component.length - sourceOffset;
        System.arraycopy(
                component,
                sourceOffset,
                target,
                targetOffset + ES256_SIGNATURE_COMPONENT_LENGTH - length,
                length
        );
    }

    private String encodeBase64Url(byte[] value) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value);
    }
}
