package com.uynguyen.jwt_spring_security.exception;

import static org.springframework.http.HttpStatus.*;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {
    USER_NOT_FOUND("USER_NOT_FOUND", "User not found with id %s", NOT_FOUND),

    PASSWORDS_MISMATCH(
        "PASSWORD_MISMATCH",
        "Passwords do not match",
        BAD_REQUEST
    ),

    CHANGE_PASSWORD_MISMATCH(
        "CHANGE_PASSWORD_MISMATCH",
        "New password and confirmed password must be the same",
        BAD_REQUEST
    ),

    INVALID_OLD_PASSWORD(
        "INVALID_OLD_PASSWORD",
        "Old password is invalid",
        BAD_REQUEST
    ),

    ACCOUNT_ALREADY_DEACTIVATED(
        "ACCOUNT_ALREADY_DEACTIVATED",
        "Account already deactivated",
        BAD_REQUEST
    ),

    ACCOUNT_ALREADY_ACTIVATED(
        "ACCOUNT_ALREADY_ACTIVATED",
        "Account already activated",
        BAD_REQUEST
    ),

    INVALID_JWT_TOKEN("INVALID_JWT_TOKEN", "Invalid JWT token", BAD_REQUEST),

    EMAIL_ALREADY_EXISTS(
        "EMAIL_ALREADY_EXISTS",
        "Email already exists",
        CONFLICT
    ),

    PHONE_ALREADY_EXISTS(
        "PHONE_ALREADY_EXISTS",
        "Phone number already exists",
        CONFLICT
    ),

    USER_DISABLED("USER_DISABLED", "User is disabled", UNAUTHORIZED),

    BAD_CREDENTIALS(
        "BAD_CREDENTIALS",
        "Username and / or password incorrect",
        UNAUTHORIZED
    ),

    USERNAME_NOT_FOUND("USERNAME_NOT_FOUND", "Username not found", NOT_FOUND),

    INTERNAL_SERVER_ERROR(
        "INTERNAL_SERVER_ERROR",
        "Internal Server Error",
        HttpStatus.INTERNAL_SERVER_ERROR
    );

    private final String code;
    private final String defaultMessage;
    private final HttpStatus status;
}
