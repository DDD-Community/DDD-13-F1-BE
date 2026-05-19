package com.f1.quiket.domain.auth.service;

import java.security.SecureRandom;
import org.springframework.stereotype.Component;

/**
 * 이메일 인증 코드 생성기
 */
@Component
public class EmailVerificationCodeGenerator {

    private static final int CODE_BOUND = 1_000_000;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    public String generate() {
        return "%06d".formatted(SECURE_RANDOM.nextInt(CODE_BOUND));
    }
}
