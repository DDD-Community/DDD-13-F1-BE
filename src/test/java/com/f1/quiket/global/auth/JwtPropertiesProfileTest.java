package com.f1.quiket.global.auth;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.validation.autoconfigure.ValidationAutoConfiguration;
import org.springframework.context.annotation.Configuration;

class JwtPropertiesProfileTest {

    private static final String LOCAL_SECRET = "local-dev-jwt-secret-for-quiket-f1-authentication";
    private static final String PROD_SECRET = "prod-jwt-secret-for-binding-test";

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withInitializer(new ConfigDataApplicationContextInitializer())
            .withConfiguration(AutoConfigurations.of(ValidationAutoConfiguration.class))
            .withUserConfiguration(JwtPropertiesConfiguration.class);

    @Test
    void localProfile_usesDevelopmentSecretWhenEnvironmentVariableMissing() {
        contextRunner
                .withPropertyValues("spring.profiles.active=local")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context.getBean(JwtProperties.class).getSecret()).isEqualTo(LOCAL_SECRET);
                });
    }

    @Test
    void prodProfile_failsWhenJwtSecretMissing() {
        contextRunner
                .withPropertyValues("spring.profiles.active=prod")
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure())
                            .hasMessageContaining("quiket.jwt");
                });
    }

    @Test
    void prodProfile_failsWhenJwtSecretTooShort() {
        contextRunner
                .withPropertyValues(
                        "spring.profiles.active=prod",
                        "QUIKET_JWT_SECRET=short-secret"
                )
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure())
                            .hasMessageContaining("quiket.jwt");
                });
    }

    @Test
    void prodProfile_bindsJwtSecretWhenEnvironmentVariableExists() {
        contextRunner
                .withPropertyValues(
                        "spring.profiles.active=prod",
                        "QUIKET_JWT_SECRET=" + PROD_SECRET
                )
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context.getBean(JwtProperties.class).getSecret()).isEqualTo(PROD_SECRET);
                });
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(JwtProperties.class)
    static class JwtPropertiesConfiguration {
    }
}
