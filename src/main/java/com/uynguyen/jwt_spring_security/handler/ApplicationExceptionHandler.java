package com.uynguyen.jwt_spring_security.handler;

import static com.uynguyen.jwt_spring_security.exception.ErrorCode.*;

import com.uynguyen.jwt_spring_security.exception.BusinessException;
import jakarta.persistence.EntityNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@RequiredArgsConstructor
@Slf4j
public class ApplicationExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleException(
        final BusinessException e
    ) {
        final ErrorResponse body = ErrorResponse.builder()
            .code(e.getErrorCode().getCode())
            .message(e.getMessage())
            .build();

        log.info("Business exception: {}", e.getMessage());
        log.debug(e.getMessage(), e);

        return ResponseEntity.status(
            Optional.ofNullable(e.getErrorCode().getStatus()).orElse(
                HttpStatus.BAD_REQUEST
            )
        ).body(body);
    }

    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<ErrorResponse> handleBusiness() {
        final ErrorResponse body = ErrorResponse.builder()
            .code(USER_DISABLED.getCode())
            .message(USER_DISABLED.getDefaultMessage())
            .build();

        return ResponseEntity.status(USER_DISABLED.getStatus()).body(body);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleException(
        final MethodArgumentNotValidException exp
    ) {
        final List<ValidationError> errors = new ArrayList<>();

        exp
            .getBindingResult()
            .getAllErrors()
            .forEach(error -> {
                final String fieldName = ((FieldError) error).getField();
                final String errorCode = error.getDefaultMessage();
                errors.add(
                    ValidationError.builder()
                        .field(fieldName)
                        .code(errorCode)
                        .message(errorCode)
                        .build()
                );
            });

        final ErrorResponse errorResponse = ErrorResponse.builder()
            .validationErrorList(errors)
            .build();

        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleException(
        final BadCredentialsException exception
    ) {
        log.debug(exception.getMessage(), exception);

        final ErrorResponse response = ErrorResponse.builder()
            .message(BAD_CREDENTIALS.getDefaultMessage())
            .code(BAD_CREDENTIALS.getCode())
            .build();

        return new ResponseEntity<>(response, BAD_CREDENTIALS.getStatus());
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleException(
        final EntityNotFoundException exception
    ) {
        log.debug(exception.getMessage(), exception);

        final ErrorResponse errorResponse = ErrorResponse.builder()
            .code("TBD")
            .message(exception.getMessage())
            .build();

        return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(UsernameNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleException(
        final UsernameNotFoundException exception
    ) {
        log.debug(exception.getMessage(), exception);

        final ErrorResponse response = ErrorResponse.builder()
            .code(USERNAME_NOT_FOUND.getCode())
            .message(USERNAME_NOT_FOUND.getDefaultMessage())
            .build();

        return new ResponseEntity<>(response, USERNAME_NOT_FOUND.getStatus());
    }

    @ExceptionHandler(AuthorizationDeniedException.class)
    public ResponseEntity<ErrorResponse> handleException(
        final AuthorizationDeniedException exception
    ) {
        log.debug(exception.getMessage(), exception);

        final ErrorResponse response = ErrorResponse.builder()
            .message("You are not authorized to perform this operation")
            .build();

        return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(
        final Exception exception
    ) {
        log.error(exception.getMessage(), exception);

        final ErrorResponse response = ErrorResponse.builder()
            .code(INTERNAL_SERVER_ERROR.getCode())
            .message(INTERNAL_SERVER_ERROR.getDefaultMessage())
            .build();

        return new ResponseEntity<>(
            response,
            INTERNAL_SERVER_ERROR.getStatus()
        );
    }
}
