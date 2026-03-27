package com.uynguyen.aegis_id.security;

import com.uynguyen.aegis_id.exception.BusinessException;
import com.uynguyen.aegis_id.exception.ErrorCode;
import java.net.URI;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class RedirectUriValidator {

    private final List<String> allowedRedirectUris;

    public RedirectUriValidator(
        @Value(
            "${app.security.allowed-redirect-uris}"
        ) String allowedRedirectUris
    ) {
        this.allowedRedirectUris = List.of(allowedRedirectUris.split(","));
    }

    public void validate(String redirectUri) {
        if (redirectUri == null || redirectUri.isBlank()) {
            throw new BusinessException(
                ErrorCode.INVALID_REDIRECT_URI,
                "(empty)"
            );
        }

        URI parsed;
        try {
            parsed = URI.create(redirectUri);
        } catch (IllegalArgumentException e) {
            throw new BusinessException(
                ErrorCode.INVALID_REDIRECT_URI,
                redirectUri
            );
        }

        if (parsed.getScheme() == null || parsed.getHost() == null) {
            throw new BusinessException(
                ErrorCode.INVALID_REDIRECT_URI,
                redirectUri
            );
        }

        if (
            !"https".equals(parsed.getScheme()) &&
            !"http".equals(parsed.getScheme())
        ) {
            throw new BusinessException(
                ErrorCode.INVALID_REDIRECT_URI,
                redirectUri
            );
        }

        boolean matches = allowedRedirectUris
            .stream()
            .map(String::trim)
            .anyMatch(allowed -> matchesAllowedUri(parsed, allowed));

        if (!matches) {
            throw new BusinessException(
                ErrorCode.INVALID_REDIRECT_URI,
                redirectUri
            );
        }
    }

    private boolean matchesAllowedUri(URI redirectUri, String allowedUri) {
        URI allowed = URI.create(allowedUri);
        return (
            redirectUri.getScheme().equals(allowed.getScheme()) &&
            redirectUri.getHost().equals(allowed.getHost()) &&
            redirectUri.getPort() == allowed.getPort() &&
            redirectUri.getPath().equals(allowed.getPath())
        );
    }
}
