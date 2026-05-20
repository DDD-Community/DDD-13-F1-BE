package com.f1.quiket.support.auth;

import com.f1.quiket.domain.auth.service.LocalAuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class EmailVerificationCleanupScheduler {

    private final LocalAuthService localAuthService;

    @Scheduled(fixedDelayString = "${quiket.auth.email-verification-cleanup-fixed-delay-ms:600000}")
    public void expirePendingEmailVerifications() {
        localAuthService.expirePendingEmailVerifications();
    }
}
