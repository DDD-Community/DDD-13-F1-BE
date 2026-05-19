package com.f1.quiket.domain.auth.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class EmailAvailabilityResponse {

    private final String email;
    private final boolean available;

    public static EmailAvailabilityResponse of(String email, boolean available) {
        return EmailAvailabilityResponse.builder()
                .email(email)
                .available(available)
                .build();
    }
}
