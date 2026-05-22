package com.f1.quiket.domain.auth.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.f1.quiket.domain.user.entity.User;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LoginFailureResponse {

    private final String email;
    private final int failedLoginCount;
    private final int remainingAttempts;
    private final boolean passwordResetRequired;
    private final String nextAction;
    private final Boolean resetCodeSent;

    public static LoginFailureResponse of(User user, int maxFailedLoginCount, boolean resetCodeSent) {
        boolean passwordResetRequired = user.getFailedLoginCount() >= maxFailedLoginCount || user.isLocked();
        return LoginFailureResponse.builder()
                .email(user.getEmail())
                .failedLoginCount(user.getFailedLoginCount())
                .remainingAttempts(Math.max(maxFailedLoginCount - user.getFailedLoginCount(), 0))
                .passwordResetRequired(passwordResetRequired)
                .nextAction(passwordResetRequired ? "password_reset" : null)
                .resetCodeSent(passwordResetRequired ? resetCodeSent : null)
                .build();
    }
}
