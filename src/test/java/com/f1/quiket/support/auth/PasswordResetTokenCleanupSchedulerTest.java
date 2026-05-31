package com.f1.quiket.support.auth;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.f1.quiket.domain.auth.service.PasswordResetService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PasswordResetTokenCleanupSchedulerTest {

    private PasswordResetService passwordResetService;
    private PasswordResetTokenCleanupScheduler scheduler;

    @BeforeEach
    void setUp() {
        passwordResetService = mock(PasswordResetService.class);
        scheduler = new PasswordResetTokenCleanupScheduler(passwordResetService);
    }

    @Test
    void expirePendingPasswordResetTokens_delegates_to_service() {
        when(passwordResetService.expirePendingPasswordResetTokens()).thenReturn(2);

        scheduler.expirePendingPasswordResetTokens();

        verify(passwordResetService).expirePendingPasswordResetTokens();
    }

    @Test
    void expirePendingPasswordResetTokens_runs_when_nothing_to_clean() {
        when(passwordResetService.expirePendingPasswordResetTokens()).thenReturn(0);

        assertThatCode(() -> scheduler.expirePendingPasswordResetTokens()).doesNotThrowAnyException();
        verify(passwordResetService).expirePendingPasswordResetTokens();
    }

    @Test
    void expirePendingPasswordResetTokens_isolates_exception() {
        // 정리 실패가 스케줄러를 멈추지 않도록 예외를 격리한다(ERROR 로그만).
        when(passwordResetService.expirePendingPasswordResetTokens()).thenThrow(new RuntimeException("db down"));

        assertThatCode(() -> scheduler.expirePendingPasswordResetTokens()).doesNotThrowAnyException();
        verify(passwordResetService).expirePendingPasswordResetTokens();
    }
}
