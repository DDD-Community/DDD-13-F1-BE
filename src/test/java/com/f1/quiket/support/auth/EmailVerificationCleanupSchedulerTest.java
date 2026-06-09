package com.f1.quiket.support.auth;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.f1.quiket.domain.auth.service.LocalAuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class EmailVerificationCleanupSchedulerTest {

    private LocalAuthService localAuthService;
    private EmailVerificationCleanupScheduler scheduler;

    @BeforeEach
    void setUp() {
        localAuthService = mock(LocalAuthService.class);
        scheduler = new EmailVerificationCleanupScheduler(localAuthService);
    }

    @Test
    void expirePendingEmailVerifications_delegates_to_service() {
        when(localAuthService.expirePendingEmailVerifications()).thenReturn(3);

        scheduler.expirePendingEmailVerifications();

        verify(localAuthService).expirePendingEmailVerifications();
    }

    @Test
    void expirePendingEmailVerifications_runs_when_nothing_to_clean() {
        when(localAuthService.expirePendingEmailVerifications()).thenReturn(0);

        assertThatCode(() -> scheduler.expirePendingEmailVerifications()).doesNotThrowAnyException();
        verify(localAuthService).expirePendingEmailVerifications();
    }

    @Test
    void expirePendingEmailVerifications_isolates_exception() {
        // 정리 실패 시 예외 격리로 스케줄러 중단 방지(ERROR 로그만)
        when(localAuthService.expirePendingEmailVerifications()).thenThrow(new RuntimeException("db down"));

        assertThatCode(() -> scheduler.expirePendingEmailVerifications()).doesNotThrowAnyException();
        verify(localAuthService).expirePendingEmailVerifications();
    }
}
