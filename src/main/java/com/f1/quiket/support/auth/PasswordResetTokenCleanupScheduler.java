package com.f1.quiket.support.auth;

import com.f1.quiket.domain.auth.service.PasswordResetService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 만료(expires_at 경과)된 pending 비밀번호 재설정 토큰의 expired 상태 정리 스케줄러
 *
 * <p>정리 건수 0 초과 시에만 INFO 기록(노이즈 방지), 실패 시 ERROR 노출 + 예외 격리로
 * 다음 주기 실행 보장</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PasswordResetTokenCleanupScheduler {

    private final PasswordResetService passwordResetService;

    @Scheduled(fixedDelayString = "${quiket.auth.password-reset-cleanup-fixed-delay-ms:600000}")
    public void expirePendingPasswordResetTokens() {
        try {
            int expired = passwordResetService.expirePendingPasswordResetTokens();
            if (expired > 0) {
                log.info("Expired password reset tokens cleaned up: count={}", expired);
            }
        } catch (Exception e) {
            log.error("Failed to clean up expired password reset tokens", e);
        }
    }
}
