package com.uynguyen.aegis_id.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.Environment;

@ExtendWith(MockitoExtension.class)
@DisplayName("SecurityConfig Unit Tests")
class SecurityConfigUnitTest {

    @Mock
    private Environment environment;

    @Mock
    private JwtFilter jwtFilter;

    @Test
    @DisplayName(
        "Should wrap internal failures when building security filter chain"
    )
    void shouldWrapInternalFailuresWhenBuildingSecurityFilterChain() {
        SecurityConfig securityConfig = new SecurityConfig(
            environment,
            jwtFilter
        );

        IllegalStateException exception = assertThrows(
            IllegalStateException.class,
            () -> securityConfig.filterChain(null)
        );

        assertEquals(
            "Failed to build security filter chain",
            exception.getMessage()
        );
        assertNotNull(exception.getCause());
        assertTrue(exception.getCause() instanceof NullPointerException);
    }
}
