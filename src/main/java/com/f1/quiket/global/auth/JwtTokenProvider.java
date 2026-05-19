package com.f1.quiket.global.auth;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.f1.quiket.domain.user.entity.User;
import com.f1.quiket.global.error.CustomException;
import com.f1.quiket.global.response.ErrorCode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

    private static final String HMAC_SHA256 = "HmacSHA256";
    private static final String ACCESS_TOKEN_TYPE = "access";
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    private final JwtProperties jwtProperties;
    private final ObjectMapper objectMapper;

    public String createAccessToken(User user) {
        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plusSeconds(jwtProperties.getAccessTokenExpiresInSeconds());

        Map<String, Object> header = new LinkedHashMap<>();
        header.put("alg", "HS256");
        header.put("typ", "JWT");

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("iss", jwtProperties.getIssuer());
        payload.put("sub", user.getPublicId());
        payload.put("typ", ACCESS_TOKEN_TYPE);
        payload.put("iat", issuedAt.getEpochSecond());
        payload.put("exp", expiresAt.getEpochSecond());

        String unsignedToken = encodeJson(header) + "." + encodeJson(payload);
        return unsignedToken + "." + sign(unsignedToken);
    }

    public String getSubject(String token) {
        Map<String, Object> payload = validateAndGetPayload(token);
        return payload.get("sub").toString();
    }

    public long getAccessTokenExpiresInSeconds() {
        return jwtProperties.getAccessTokenExpiresInSeconds();
    }

    private Map<String, Object> validateAndGetPayload(String token) {
        String[] parts = token.split("\\.");
        if (parts.length != 3) {
            throw new CustomException(ErrorCode.AUTH_INVALID_TOKEN);
        }

        String unsignedToken = parts[0] + "." + parts[1];
        byte[] expectedSignature = sign(unsignedToken).getBytes(StandardCharsets.UTF_8);
        byte[] actualSignature = parts[2].getBytes(StandardCharsets.UTF_8);
        if (!MessageDigest.isEqual(expectedSignature, actualSignature)) {
            throw new CustomException(ErrorCode.AUTH_INVALID_TOKEN);
        }

        Map<String, Object> payload = decodeJson(parts[1]);
        validatePayload(payload);
        return payload;
    }

    private void validatePayload(Map<String, Object> payload) {
        if (!jwtProperties.getIssuer().equals(payload.get("iss"))
                || !ACCESS_TOKEN_TYPE.equals(payload.get("typ"))
                || payload.get("sub") == null
                || payload.get("exp") == null) {
            throw new CustomException(ErrorCode.AUTH_INVALID_TOKEN);
        }

        long expiresAt = ((Number) payload.get("exp")).longValue();
        if (Instant.now().getEpochSecond() >= expiresAt) {
            throw new CustomException(ErrorCode.AUTH_EXPIRED_TOKEN);
        }
    }

    private String encodeJson(Map<String, Object> value) {
        try {
            byte[] json = objectMapper.writeValueAsBytes(value);
            return Base64.getUrlEncoder().withoutPadding().encodeToString(json);
        } catch (Exception e) {
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR, e);
        }
    }

    private Map<String, Object> decodeJson(String value) {
        try {
            byte[] json = Base64.getUrlDecoder().decode(value);
            return objectMapper.readValue(json, MAP_TYPE);
        } catch (Exception e) {
            throw new CustomException(ErrorCode.AUTH_INVALID_TOKEN);
        }
    }

    private String sign(String unsignedToken) {
        try {
            Mac mac = Mac.getInstance(HMAC_SHA256);
            mac.init(new SecretKeySpec(jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8), HMAC_SHA256));
            byte[] signature = mac.doFinal(unsignedToken.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(signature);
        } catch (Exception e) {
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR, e);
        }
    }
}
