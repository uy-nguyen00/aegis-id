package com.uynguyen.aegis_id.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;

public class EmailDomainValidator
    implements ConstraintValidator<NonDisposableEmail, String> {

    private final Set<String> blockedDomains;

    public EmailDomainValidator(
        @Value("${app.security.disposable-email-domains}") final List<
            String
        > blockedDomains
    ) {
        this.blockedDomains = blockedDomains
            .stream()
            .map(String::toLowerCase)
            .collect(Collectors.toSet());
    }

    @Override
    public boolean isValid(
        final String email,
        final ConstraintValidatorContext constraintValidatorContext
    ) {
        if (email == null || !email.contains("@")) {
            return true;
        }

        final int atIndex = email.lastIndexOf('@') + 1;
        final int dotIndex = email.lastIndexOf('.');
        final String domain = email.substring(atIndex, dotIndex).toLowerCase();
        return !this.blockedDomains.contains(domain);
    }
}
