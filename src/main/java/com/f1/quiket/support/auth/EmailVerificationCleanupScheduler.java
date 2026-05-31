package com.f1.quiket.support.auth;

import com.f1.quiket.domain.auth.service.LocalAuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 만료(expires_at 경과)된 pending 이메일 인증 토큰을 expired 상태로 정리하는 스케줄러.
 *
 * <p>정리 건수가 있을 때만 INFO로 기록하고(0건은 미기록 — 노이즈 방지), 정리 실패는
 * ERROR로 노출하면서 예외를 격리해 다음 주기 실행이 멈추지 않도록 한다.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EmailVerificationCleanupScheduler {

    private final LocalAuthService localAuthService;

    @Scheduled(fixedDelayString = "${quiket.auth.email-verification-cleanup-fixed-delay-ms:600000}")
    public void expirePendingEmailVerifications() {
        try {
            int expired = localAuthService.expirePendingEmailVerifications();
            if (expired > 0) {
                log.info("만료된 이메일 인증 {}건을 정리했습니다.", expired);
            }
        } catch (Exception e) {
            log.error("이메일 인증 만료 정리에 실패했습니다.", e);
        }
    }
}
