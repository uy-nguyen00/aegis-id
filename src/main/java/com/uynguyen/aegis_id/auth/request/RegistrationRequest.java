package com.uynguyen.aegis_id.auth.request;

import com.uynguyen.aegis_id.validation.NonDisposableEmail;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RegistrationRequest {

    @NotBlank(message = "VALIDATION.REGISTRATION.FIRSTNAME.NOT_BLANK")
    @Size(min = 1, max = 50, message = "VALIDATION.REGISTRATION.FIRSTNAME.SIZE")
    @Pattern(
        regexp = "^[\\p{L} '-]+$",
        message = "VALIDATION.REGISTRATION.FIRSTNAME.PATTERN"
    )
    @Schema(example = "John")
    private String firstName;

    @Size(max = 50, message = "VALIDATION.REGISTRATION.LASTNAME.SIZE")
    @Pattern(
        regexp = "^[\\p{L} '-]+$",
        message = "VALIDATION.REGISTRATION.LASTNAME.PATTERN"
    )
    @Schema(example = "Doe")
    private String lastName;

    @NotBlank(message = "VALIDATION.REGISTRATION.EMAIL.BLANK")
    @Email(message = "VALIDATION.REGISTRATION.EMAIL.FORMAT")
    @NonDisposableEmail(message = "VALIDATION.REGISTRATION.EMAIL.DISPOSABLE")
    @Schema(example = "example@mail.com")
    private String email;

    @NotBlank(message = "VALIDATION.REGISTRATION.PHONE.BLANK")
    @Pattern(
        regexp = "^\\+?[1-9]\\d{1,14}$",
        message = "VALIDATION.REGISTRATION.PHONE.FORMAT"
    )
    @Schema(example = "+33123456789")
    private String phoneNumber;

    @NotBlank(message = "VALIDATION.REGISTRATION.PASSWORD.BLANK")
    @Size(min = 8, max = 72, message = "VALIDATION.REGISTRATION.PASSWORD.SIZE")
    @Pattern(
        regexp = "^(?=.*[A-Z])(?=.*[a-z])(?=.*\\d)(?=.*\\W).*$",
        message = "VALIDATION.REGISTRATION.PASSWORD.WEAK"
    )
    @Schema(example = "pAssword1!_")
    private String password;

    @NotBlank(message = "VALIDATION.REGISTRATION.CONFIRM_PASSWORD.BLANK")
    @Size(
        min = 8,
        max = 72,
        message = "VALIDATION.REGISTRATION.CONFIRM_PASSWORD.SIZE"
    )
    @Schema(example = "pAssword1!_")
    private String confirmPassword;
}
