package com.f1.quiket.support.auth;

import com.f1.quiket.domain.auth.service.LocalAuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 만료(expires_at 경과)된 pending 이메일 인증 토큰의 expired 상태 정리 스케줄러
 *
 * <p>정리 건수 0 초과 시에만 INFO 기록(노이즈 방지), 실패 시 ERROR 노출 + 예외 격리로
 * 다음 주기 실행 보장</p>
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
