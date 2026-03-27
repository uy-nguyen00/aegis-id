package com.uynguyen.aegis_id.security;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.uynguyen.aegis_id.exception.BusinessException;
import com.uynguyen.aegis_id.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class RedirectUriValidatorTest {

    private final RedirectUriValidator validator = new RedirectUriValidator(
        "http://localhost:3000/callback,https://myapp.example.com/auth/callback"
    );

    @Nested
    @DisplayName("Valid redirect URIs")
    class ValidUris {

        @Test
        @DisplayName("Should accept exact match with http")
        void shouldAcceptExactHttpMatch() {
            assertDoesNotThrow(() ->
                validator.validate("http://localhost:3000/callback")
            );
        }

        @Test
        @DisplayName("Should accept exact match with https")
        void shouldAcceptExactHttpsMatch() {
            assertDoesNotThrow(() ->
                validator.validate("https://myapp.example.com/auth/callback")
            );
        }
    }

    @Nested
    @DisplayName("Invalid redirect URIs")
    class InvalidUris {

        @Test
        @DisplayName("Should reject null URI")
        void shouldRejectNull() {
            BusinessException ex = assertThrows(BusinessException.class, () ->
                validator.validate(null)
            );
            assertEquals(ErrorCode.INVALID_REDIRECT_URI, ex.getErrorCode());
        }

        @Test
        @DisplayName("Should reject blank URI")
        void shouldRejectBlank() {
            BusinessException ex = assertThrows(BusinessException.class, () ->
                validator.validate("   ")
            );
            assertEquals(ErrorCode.INVALID_REDIRECT_URI, ex.getErrorCode());
        }

        @Test
        @DisplayName("Should reject URI not in allowlist")
        void shouldRejectUnknownUri() {
            BusinessException ex = assertThrows(BusinessException.class, () ->
                validator.validate("https://evil.com/callback")
            );
            assertEquals(ErrorCode.INVALID_REDIRECT_URI, ex.getErrorCode());
        }

        @Test
        @DisplayName("Should reject URI with different path")
        void shouldRejectDifferentPath() {
            BusinessException ex = assertThrows(BusinessException.class, () ->
                validator.validate("http://localhost:3000/other")
            );
            assertEquals(ErrorCode.INVALID_REDIRECT_URI, ex.getErrorCode());
        }

        @Test
        @DisplayName("Should reject URI with different port")
        void shouldRejectDifferentPort() {
            BusinessException ex = assertThrows(BusinessException.class, () ->
                validator.validate("http://localhost:8080/callback")
            );
            assertEquals(ErrorCode.INVALID_REDIRECT_URI, ex.getErrorCode());
        }

        @Test
        @DisplayName("Should reject URI with different scheme")
        void shouldRejectDifferentScheme() {
            BusinessException ex = assertThrows(BusinessException.class, () ->
                validator.validate("https://localhost:3000/callback")
            );
            assertEquals(ErrorCode.INVALID_REDIRECT_URI, ex.getErrorCode());
        }

        @Test
        @DisplayName("Should reject non-http scheme")
        void shouldRejectNonHttpScheme() {
            BusinessException ex = assertThrows(BusinessException.class, () ->
                validator.validate("ftp://localhost:3000/callback")
            );
            assertEquals(ErrorCode.INVALID_REDIRECT_URI, ex.getErrorCode());
        }

        @Test
        @DisplayName("Should reject malformed URI")
        void shouldRejectMalformedUri() {
            BusinessException ex = assertThrows(BusinessException.class, () ->
                validator.validate("://not-valid")
            );
            assertEquals(ErrorCode.INVALID_REDIRECT_URI, ex.getErrorCode());
        }
    }
}
