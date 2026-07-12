package com.f1.quiket.global.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Getter
@Setter
@Component
@Validated
@ConfigurationProperties(prefix = "quiket.jwt")
public class JwtProperties {

    @NotBlank
    private String issuer;

    @NotBlank
    @Size(min = 32)
    private String secret;

    @Positive
    private long accessTokenExpiresInSeconds;

    @Positive
    private long refreshTokenExpiresInSeconds;
}
