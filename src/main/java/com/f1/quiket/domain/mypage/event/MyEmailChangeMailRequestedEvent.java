package com.f1.quiket.domain.mypage.event;

public record MyEmailChangeMailRequestedEvent(
        String email,
        String verificationCode
) {
}
