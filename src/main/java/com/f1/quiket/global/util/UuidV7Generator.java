package com.f1.quiket.global.util;

import java.security.SecureRandom;
import java.util.UUID;

/**
 * UUID v7 생성기
 */
public final class UuidV7Generator {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private UuidV7Generator() {
    }

    public static String generate() {
        long timestampMillis = System.currentTimeMillis();
        long randomA = SECURE_RANDOM.nextLong() & 0x0FFFL;
        long randomB = SECURE_RANDOM.nextLong() & 0x3FFF_FFFF_FFFF_FFFFL;

        long mostSignificantBits = (timestampMillis << 16)
                | 0x7000L
                | randomA;
        long leastSignificantBits = 0x8000_0000_0000_0000L | randomB;

        return new UUID(mostSignificantBits, leastSignificantBits).toString();
    }
}
