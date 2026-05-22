package com.f1.quiket.support.auth;

import com.f1.quiket.domain.auth.service.PasswordResetService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PasswordResetTokenCleanupScheduler {

    private final PasswordResetService passwordResetService;

    @Scheduled(fixedDelayString = "${quiket.auth.password-reset-cleanup-fixed-delay-ms:600000}")
    public void expirePendingPasswordResetTokens() {
        passwordResetService.expirePendingPasswordResetTokens();
    }
}
