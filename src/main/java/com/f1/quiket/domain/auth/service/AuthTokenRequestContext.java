package com.f1.quiket.domain.auth.service;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AuthTokenRequestContext {

    private final String deviceId;
    private final String deviceName;
    private final String userAgent;
    private final String ipAddress;
}
