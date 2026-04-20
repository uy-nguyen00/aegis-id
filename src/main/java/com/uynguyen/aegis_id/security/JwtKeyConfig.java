package com.uynguyen.aegis_id.security;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.util.StringUtils;

@EnableConfigurationProperties(JwtKeyProperties.class)
@Configuration
@RequiredArgsConstructor
public class JwtKeyConfig {

    private static final String[] PRIVATE_KEY_HEADERS = {
        "-----BEGIN PRIVATE KEY-----",
        "-----BEGIN RSA PRIVATE KEY-----",
    };
    private static final String[] PRIVATE_KEY_FOOTERS = {
        "-----END PRIVATE KEY-----",
        "-----END RSA PRIVATE KEY-----",
    };
    private static final String[] PUBLIC_KEY_HEADERS = {
        "-----BEGIN PUBLIC KEY-----",
        "-----BEGIN RSA PUBLIC KEY-----",
    };
    private static final String[] PUBLIC_KEY_FOOTERS = {
        "-----END PUBLIC KEY-----",
        "-----END RSA PUBLIC KEY-----",
    };

    private static final KeyFactory RSA_KEY_FACTORY = createKeyFactory();

    private final JwtKeyProperties jwtKeyProperties;

    @Bean
    public RSAPrivateKey jwtPrivateKey() {
        final byte[] privateKeyBytes = readAndDecodeKey(
            this.jwtKeyProperties.getPrivateKeyLocation(),
            "app.security.jwt.private-key-location",
            PRIVATE_KEY_HEADERS,
            PRIVATE_KEY_FOOTERS
        );
        try {
            return (RSAPrivateKey) RSA_KEY_FACTORY.generatePrivate(
                new PKCS8EncodedKeySpec(privateKeyBytes)
            );
        } catch (InvalidKeySpecException | ClassCastException e) {
            throw new IllegalArgumentException(
                "Invalid value for app.security.jwt.private-key-location: " +
                    "must resolve to a PEM-encoded PKCS#8 RSA private key",
                e
            );
        }
    }

    @Bean
    public RSAPublicKey jwtPublicKey() {
        final byte[] publicKeyBytes = readAndDecodeKey(
            this.jwtKeyProperties.getPublicKeyLocation(),
            "app.security.jwt.public-key-location",
            PUBLIC_KEY_HEADERS,
            PUBLIC_KEY_FOOTERS
        );
        try {
            return (RSAPublicKey) RSA_KEY_FACTORY.generatePublic(
                new X509EncodedKeySpec(publicKeyBytes)
            );
        } catch (InvalidKeySpecException | ClassCastException e) {
            throw new IllegalArgumentException(
                "Invalid value for app.security.jwt.public-key-location: " +
                    "must resolve to a PEM-encoded X.509 RSA public key",
                e
            );
        }
    }

    private byte[] readAndDecodeKey(
        final Resource keyResource,
        final String propertyName,
        final String[] expectedHeaders,
        final String[] expectedFooters
    ) {
        final String keyContent;
        try (InputStream keyInputStream = keyResource.getInputStream()) {
            keyContent = new String(
                keyInputStream.readAllBytes(),
                StandardCharsets.UTF_8
            );
        } catch (IOException e) {
            throw new IllegalStateException(
                "Failed to read " + propertyName + " from " + keyResource,
                e
            );
        }

        final String sanitizedPem = stripPemHeadersAndFooters(
            keyContent,
            propertyName,
            expectedHeaders,
            expectedFooters
        ).replaceAll("\\s+", "");

        if (!StringUtils.hasText(sanitizedPem)) {
            throw new IllegalArgumentException(
                "Invalid value for " +
                    propertyName +
                    ": resolved resource is empty or missing PEM key content"
            );
        }

        try {
            return Base64.getDecoder().decode(sanitizedPem);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                "Invalid value for " +
                    propertyName +
                    ": key content is not valid PEM/base64",
                e
            );
        }
    }

    private String stripPemHeadersAndFooters(
        final String keyContent,
        final String propertyName,
        final String[] expectedHeaders,
        final String[] expectedFooters
    ) {
        final boolean containsPemDelimiters =
            keyContent.contains("-----BEGIN") ||
            keyContent.contains("-----END");

        String strippedKeyContent = keyContent;
        boolean hasExpectedDelimiter = false;

        for (int i = 0; i < expectedHeaders.length; i++) {
            final String header = expectedHeaders[i];
            final String footer = expectedFooters[i];

            if (
                strippedKeyContent.contains(header) ||
                strippedKeyContent.contains(footer)
            ) {
                hasExpectedDelimiter = true;
            }

            strippedKeyContent = strippedKeyContent
                .replace(header, "")
                .replace(footer, "");
        }

        final boolean containsUnexpectedDelimiters =
            strippedKeyContent.contains("-----BEGIN") ||
            strippedKeyContent.contains("-----END");

        if (
            (containsPemDelimiters && !hasExpectedDelimiter) ||
            containsUnexpectedDelimiters
        ) {
            throw new IllegalArgumentException(
                "Invalid value for " +
                    propertyName +
                    ": resolved resource contains unexpected PEM header/footer"
            );
        }

        return strippedKeyContent;
    }

    private static KeyFactory createKeyFactory() {
        try {
            return KeyFactory.getInstance("RSA");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(
                "RSA algorithm is not available in this JVM",
                e
            );
        }
    }
}
