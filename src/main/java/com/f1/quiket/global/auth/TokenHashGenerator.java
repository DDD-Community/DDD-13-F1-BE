package com.f1.quiket.global.auth;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import org.springframework.stereotype.Component;

@Component
public class TokenHashGenerator {

    private static final String SHA_256 = "SHA-256";

    public String hash(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance(SHA_256);
            byte[] hashedToken = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hashedToken);
        } catch (Exception e) {
            throw new IllegalStateException("토큰 해시 생성 실패", e);
        }
    }
}
