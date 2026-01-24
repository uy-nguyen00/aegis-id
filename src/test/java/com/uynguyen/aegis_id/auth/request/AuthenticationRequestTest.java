package com.uynguyen.aegis_id.auth.request;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class AuthenticationRequestTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    void testValidAuthenticationRequest() {
        // Given
        AuthenticationRequest request = AuthenticationRequest.builder()
                .email("test@example.com")
                .password("password123")
                .build();

        // When
        Set<ConstraintViolation<AuthenticationRequest>> violations = validator.validate(request);

        // Then
        assertTrue(violations.isEmpty(), "Valid request should have no violations");
    }

    @Test
    void testEmailCannotBeBlank() {
        // Given
        AuthenticationRequest request = AuthenticationRequest.builder()
                .email("")
                .password("password123")
                .build();

        // When
        Set<ConstraintViolation<AuthenticationRequest>> violations = validator.validate(request);

        // Then
        assertEquals(1, violations.size());
        ConstraintViolation<AuthenticationRequest> violation = violations.iterator().next();
        assertEquals("email", violation.getPropertyPath().toString());
        assertEquals("VALIDATION.AUTHENTICATION.EMAIL.NOT_BLANK", violation.getMessage());
    }

    @Test
    void testEmailCannotBeNull() {
        // Given
        AuthenticationRequest request = AuthenticationRequest.builder()
                .email(null)
                .password("password123")
                .build();

        // When
        Set<ConstraintViolation<AuthenticationRequest>> violations = validator.validate(request);

        // Then
        assertEquals(1, violations.size());
        ConstraintViolation<AuthenticationRequest> violation = violations.iterator().next();
        assertEquals("email", violation.getPropertyPath().toString());
        assertEquals("VALIDATION.AUTHENTICATION.EMAIL.NOT_BLANK", violation.getMessage());
    }

    @Test
    void testEmailMustBeValidFormat() {
        // Given
        AuthenticationRequest request = AuthenticationRequest.builder()
                .email("invalid-email")
                .password("password123")
                .build();

        // When
        Set<ConstraintViolation<AuthenticationRequest>> violations = validator.validate(request);

        // Then
        assertEquals(1, violations.size());
        ConstraintViolation<AuthenticationRequest> violation = violations.iterator().next();
        assertEquals("email", violation.getPropertyPath().toString());
        assertEquals("VALIDATION.AUTHENTICATION.EMAIL.FORMAT", violation.getMessage());
    }

    @Test
    void testPasswordCannotBeBlank() {
        // Given
        AuthenticationRequest request = AuthenticationRequest.builder()
                .email("test@example.com")
                .password("")
                .build();

        // When
        Set<ConstraintViolation<AuthenticationRequest>> violations = validator.validate(request);

        // Then
        assertEquals(1, violations.size());
        ConstraintViolation<AuthenticationRequest> violation = violations.iterator().next();
        assertEquals("password", violation.getPropertyPath().toString());
        assertEquals("VALIDATION.AUTHENTICATION.PASSWORD.NOT_BLANK", violation.getMessage());
    }

    @Test
    void testPasswordCannotBeNull() {
        // Given
        AuthenticationRequest request = AuthenticationRequest.builder()
                .email("test@example.com")
                .password(null)
                .build();

        // When
        Set<ConstraintViolation<AuthenticationRequest>> violations = validator.validate(request);

        // Then
        assertEquals(1, violations.size());
        ConstraintViolation<AuthenticationRequest> violation = violations.iterator().next();
        assertEquals("password", violation.getPropertyPath().toString());
        assertEquals("VALIDATION.AUTHENTICATION.PASSWORD.NOT_BLANK", violation.getMessage());
    }

    @Test
    void testMultipleValidationErrors() {
        // Given
        AuthenticationRequest request = AuthenticationRequest.builder()
                .email("invalid-email")
                .password("")
                .build();

        // When
        Set<ConstraintViolation<AuthenticationRequest>> violations = validator.validate(request);

        // Then
        assertEquals(2, violations.size(), "Should have 2 validation errors");
    }

    @Test
    void testBuilderPattern() {
        // Given & When
        AuthenticationRequest request = AuthenticationRequest.builder()
                .email("test@example.com")
                .password("password123")
                .build();

        // Then
        assertNotNull(request);
        assertEquals("test@example.com", request.getEmail());
        assertEquals("password123", request.getPassword());
    }

    @Test
    void testNoArgsConstructor() {
        // Given & When
        AuthenticationRequest request = new AuthenticationRequest();

        // Then
        assertNotNull(request);
        assertNull(request.getEmail());
        assertNull(request.getPassword());
    }

    @Test
    void testAllArgsConstructor() {
        // Given & When
        AuthenticationRequest request = new AuthenticationRequest("test@example.com", "password123");

        // Then
        assertNotNull(request);
        assertEquals("test@example.com", request.getEmail());
        assertEquals("password123", request.getPassword());
    }

    @Test
    void testGettersAndSetters() {
        // Given
        AuthenticationRequest request = new AuthenticationRequest();

        // When
        request.setEmail("test@example.com");
        request.setPassword("password123");

        // Then
        assertEquals("test@example.com", request.getEmail());
        assertEquals("password123", request.getPassword());
    }
}