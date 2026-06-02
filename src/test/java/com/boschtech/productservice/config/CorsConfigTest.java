package com.boschtech.productservice.config;

import org.junit.jupiter.api.Test;
import org.springframework.web.cors.CorsConfigurationSource;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class CorsConfigTest {

    @Test
    void corsConfigurationSource_shouldCreateSourceWithConfiguredOrigins() throws Exception {
        CorsConfig config = new CorsConfig();

        // Inject the @Value-bound field via reflection (no Spring context needed)
        Field originsField = CorsConfig.class.getDeclaredField("allowedOrigins");
        originsField.setAccessible(true);
        originsField.set(config, "http://localhost:3000,http://localhost:4200");

        CorsConfigurationSource source = config.corsConfigurationSource();

        assertNotNull(source);
    }
}
