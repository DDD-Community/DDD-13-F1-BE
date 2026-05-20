package com.f1.quiket.domain.auth.event;

public record EmailVerificationMailRequestedEvent(
        String email,
        String verificationCode
) {
}
