package com.f1.quiket.domain.mypage.service;

public record MyEmailChangeVerificationPayload(
        String newEmail,
        String verificationCode
) {
}
