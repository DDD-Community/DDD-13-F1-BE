package com.f1.quiket.domain.auth.event;

public record PasswordResetMailRequestedEvent(
        String email,
        String verificationCode
) {
}
