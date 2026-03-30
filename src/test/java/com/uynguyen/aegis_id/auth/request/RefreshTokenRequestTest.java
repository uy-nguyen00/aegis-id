package com.uynguyen.aegis_id.auth.request;

import static org.junit.jupiter.api.Assertions.*;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class RefreshTokenRequestTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    void testValidRefreshTokenRequest() {
        // Given
        RefreshTokenRequest request = RefreshTokenRequest.builder()
            .refreshToken("valid-refresh-token-123")
            .build();

        // When
        Set<ConstraintViolation<RefreshTokenRequest>> violations =
            validator.validate(request);

        // Then
        assertTrue(
            violations.isEmpty(),
            "Valid request should have no violations"
        );
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = { "   " })
    void testRefreshTokenCannotBeBlankOrNullOrWhitespace(String refreshToken) {
        // Given
        RefreshTokenRequest request = RefreshTokenRequest.builder()
            .refreshToken(refreshToken)
            .build();

        // When
        Set<ConstraintViolation<RefreshTokenRequest>> violations =
            validator.validate(request);

        // Then
        assertEquals(1, violations.size());
        ConstraintViolation<RefreshTokenRequest> violation = violations
            .iterator()
            .next();
        assertEquals("refreshToken", violation.getPropertyPath().toString());
        assertEquals(
            "VALIDATION.REFRESH_TOKEN.NOT_BLANK",
            violation.getMessage()
        );
    }

    @Test
    void testBuilderPattern() {
        // Given & When
        RefreshTokenRequest request = RefreshTokenRequest.builder()
            .refreshToken("test-refresh-token")
            .build();

        // Then
        assertNotNull(request);
        assertEquals("test-refresh-token", request.getRefreshToken());
    }

    @Test
    void testNoArgsConstructor() {
        // Given & When
        RefreshTokenRequest request = new RefreshTokenRequest();

        // Then
        assertNotNull(request);
        assertNull(request.getRefreshToken());
    }

    @Test
    void testAllArgsConstructor() {
        // Given & When
        RefreshTokenRequest request = new RefreshTokenRequest(
            "test-refresh-token"
        );

        // Then
        assertNotNull(request);
        assertEquals("test-refresh-token", request.getRefreshToken());
    }

    @Test
    void testGettersAndSetters() {
        // Given
        RefreshTokenRequest request = new RefreshTokenRequest();

        // When
        request.setRefreshToken("test-refresh-token");

        // Then
        assertEquals("test-refresh-token", request.getRefreshToken());
    }
}
