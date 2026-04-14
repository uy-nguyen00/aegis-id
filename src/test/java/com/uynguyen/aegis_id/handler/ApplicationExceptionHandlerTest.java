package com.uynguyen.aegis_id.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.uynguyen.aegis_id.exception.BusinessException;
import com.uynguyen.aegis_id.exception.ErrorCode;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.ConstraintViolationException;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

@DisplayName("ApplicationExceptionHandler Unit Tests")
class ApplicationExceptionHandlerTest {

    private final ApplicationExceptionHandler handler =
        new ApplicationExceptionHandler();

    @Test
    @DisplayName("Should map BusinessException to response body and status")
    void shouldMapBusinessExceptionToResponseBodyAndStatus() {
        BusinessException exception = new BusinessException(
            ErrorCode.USER_NOT_FOUND,
            "user-123"
        );

        ResponseEntity<ErrorResponse> response = handler.handleException(
            exception
        );

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(
            ErrorCode.USER_NOT_FOUND.getCode(),
            response.getBody().getCode()
        );
        assertEquals(
            "User not found with id user-123",
            response.getBody().getMessage()
        );
    }

    @Test
    @DisplayName("Should map DisabledException to USER_DISABLED response")
    void shouldMapDisabledExceptionToUserDisabledResponse() {
        ResponseEntity<ErrorResponse> response = handler.handleBusiness();

        assertEquals(
            ErrorCode.USER_DISABLED.getStatus(),
            response.getStatusCode()
        );
        assertNotNull(response.getBody());
        assertEquals(
            ErrorCode.USER_DISABLED.getCode(),
            response.getBody().getCode()
        );
        assertEquals(
            ErrorCode.USER_DISABLED.getDefaultMessage(),
            response.getBody().getMessage()
        );
    }

    @Test
    @DisplayName(
        "Should format field validation errors from MethodArgumentNotValidException"
    )
    void shouldFormatFieldValidationErrorsFromMethodArgumentNotValidException() {
        MethodArgumentNotValidException exception = mock(
            MethodArgumentNotValidException.class
        );
        BindingResult bindingResult = new BeanPropertyBindingResult(
            new Object(),
            "registrationRequest"
        );
        bindingResult.addError(
            new FieldError(
                "registrationRequest",
                "email",
                "VALIDATION.REGISTRATION.EMAIL.FORMAT"
            )
        );
        bindingResult.addError(
            new FieldError(
                "registrationRequest",
                "password",
                "VALIDATION.REGISTRATION.PASSWORD.WEAK"
            )
        );
        when(exception.getBindingResult()).thenReturn(bindingResult);

        ResponseEntity<ErrorResponse> response = handler.handleException(
            exception
        );

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(
            ErrorCode.VALIDATION_ERROR.getCode(),
            response.getBody().getCode()
        );
        assertEquals(
            ErrorCode.VALIDATION_ERROR.getDefaultMessage(),
            response.getBody().getMessage()
        );
        assertNotNull(response.getBody().getValidationErrorList());
        assertEquals(2, response.getBody().getValidationErrorList().size());

        ValidationError firstError = response
            .getBody()
            .getValidationErrorList()
            .get(0);
        assertEquals("email", firstError.getField());
        assertEquals(
            "VALIDATION.REGISTRATION.EMAIL.FORMAT",
            firstError.getCode()
        );
        assertEquals(
            "VALIDATION.REGISTRATION.EMAIL.FORMAT",
            firstError.getMessage()
        );
    }

    @Test
    @DisplayName(
        "Should map BadCredentialsException to BAD_CREDENTIALS response"
    )
    void shouldMapBadCredentialsExceptionToBadCredentialsResponse() {
        ResponseEntity<ErrorResponse> response = handler.handleException(
            new BadCredentialsException("Invalid credentials")
        );

        assertEquals(
            ErrorCode.BAD_CREDENTIALS.getStatus(),
            response.getStatusCode()
        );
        assertNotNull(response.getBody());
        assertEquals(
            ErrorCode.BAD_CREDENTIALS.getCode(),
            response.getBody().getCode()
        );
        assertEquals(
            ErrorCode.BAD_CREDENTIALS.getDefaultMessage(),
            response.getBody().getMessage()
        );
    }

    @Test
    @DisplayName("Should map EntityNotFoundException to not found response")
    void shouldMapEntityNotFoundExceptionToNotFoundResponse() {
        ResponseEntity<ErrorResponse> response = handler.handleException(
            new EntityNotFoundException("Entity missing")
        );

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("TBD", response.getBody().getCode());
        assertEquals("Entity missing", response.getBody().getMessage());
    }

    @Test
    @DisplayName(
        "Should map UsernameNotFoundException to USERNAME_NOT_FOUND response"
    )
    void shouldMapUsernameNotFoundExceptionToUsernameNotFoundResponse() {
        ResponseEntity<ErrorResponse> response = handler.handleException(
            new UsernameNotFoundException("Unknown username")
        );

        assertEquals(
            ErrorCode.USERNAME_NOT_FOUND.getStatus(),
            response.getStatusCode()
        );
        assertNotNull(response.getBody());
        assertEquals(
            ErrorCode.USERNAME_NOT_FOUND.getCode(),
            response.getBody().getCode()
        );
        assertEquals(
            ErrorCode.USERNAME_NOT_FOUND.getDefaultMessage(),
            response.getBody().getMessage()
        );
    }

    @Test
    @DisplayName(
        "Should map AuthorizationDeniedException to unauthorized response"
    )
    void shouldMapAuthorizationDeniedExceptionToUnauthorizedResponse() {
        ResponseEntity<ErrorResponse> response = handler.handleException(
            new AuthorizationDeniedException("Forbidden")
        );

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertNull(response.getBody().getCode());
        assertEquals(
            "You are not authorized to perform this operation",
            response.getBody().getMessage()
        );
    }

    @Test
    @DisplayName("Should map malformed JSON exception through generic handler")
    void shouldMapMalformedJsonExceptionThroughGenericHandler() {
        HttpInputMessage inputMessage = mock(HttpInputMessage.class);
        HttpMessageNotReadableException exception =
            new HttpMessageNotReadableException(
                "Malformed JSON request",
                inputMessage
            );

        ResponseEntity<ErrorResponse> response = handler.handleException(
            (Exception) exception
        );

        assertEquals(
            ErrorCode.INTERNAL_SERVER_ERROR.getStatus(),
            response.getStatusCode()
        );
        assertNotNull(response.getBody());
        assertEquals(
            ErrorCode.INTERNAL_SERVER_ERROR.getCode(),
            response.getBody().getCode()
        );
        assertEquals(
            ErrorCode.INTERNAL_SERVER_ERROR.getDefaultMessage(),
            response.getBody().getMessage()
        );
    }

    @Test
    @DisplayName(
        "Should map constraint violation exception through generic handler"
    )
    void shouldMapConstraintViolationExceptionThroughGenericHandler() {
        ConstraintViolationException exception =
            new ConstraintViolationException(
                "Invalid query parameter",
                Set.of()
            );

        ResponseEntity<ErrorResponse> response = handler.handleException(
            (Exception) exception
        );

        assertEquals(
            ErrorCode.INTERNAL_SERVER_ERROR.getStatus(),
            response.getStatusCode()
        );
        assertNotNull(response.getBody());
        assertEquals(
            ErrorCode.INTERNAL_SERVER_ERROR.getCode(),
            response.getBody().getCode()
        );
        assertEquals(
            ErrorCode.INTERNAL_SERVER_ERROR.getDefaultMessage(),
            response.getBody().getMessage()
        );
    }

    @Test
    @DisplayName(
        "Should map unexpected exception to INTERNAL_SERVER_ERROR response"
    )
    void shouldMapUnexpectedExceptionToInternalServerErrorResponse() {
        ResponseEntity<ErrorResponse> response = handler.handleException(
            new RuntimeException("Unexpected failure")
        );

        assertEquals(
            ErrorCode.INTERNAL_SERVER_ERROR.getStatus(),
            response.getStatusCode()
        );
        assertNotNull(response.getBody());
        assertEquals(
            ErrorCode.INTERNAL_SERVER_ERROR.getCode(),
            response.getBody().getCode()
        );
        assertEquals(
            ErrorCode.INTERNAL_SERVER_ERROR.getDefaultMessage(),
            response.getBody().getMessage()
        );
    }
}
