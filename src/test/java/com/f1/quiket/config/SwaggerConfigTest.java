package com.f1.quiket.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class SwaggerConfigTest {

    @Test
    void openApiVersion_matchesStaticSpecVersion() {
        SwaggerConfig swaggerConfig = new SwaggerConfig();

        assertEquals("1.1.0", swaggerConfig.openAPI().getInfo().getVersion());
    }
}
