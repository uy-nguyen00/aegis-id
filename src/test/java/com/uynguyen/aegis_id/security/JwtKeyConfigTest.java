package com.uynguyen.aegis_id.security;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Base64;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.AbstractResource;
import org.springframework.core.io.ByteArrayResource;

@DisplayName("JwtKeyConfig Unit Tests")
class JwtKeyConfigTest {

    private JwtKeyProperties jwtKeyProperties;
    private RSAPrivateKey privateKey;
    private RSAPublicKey publicKey;
    private String privateKeyPem;
    private String privateKeyPemWithRsaHeader;
    private String publicKeyPem;
    private String publicKeyPemWithRsaHeader;

    @BeforeEach
    void setUp() throws NoSuchAlgorithmException {
        final KeyPair keyPair = generateRsaKeyPair();

        this.privateKey = (RSAPrivateKey) keyPair.getPrivate();
        this.publicKey = (RSAPublicKey) keyPair.getPublic();
        this.privateKeyPem = toPem(
            "-----BEGIN PRIVATE KEY-----",
            this.privateKey.getEncoded(),
            "-----END PRIVATE KEY-----"
        );
        this.privateKeyPemWithRsaHeader = toPem(
            "-----BEGIN RSA PRIVATE KEY-----",
            this.privateKey.getEncoded(),
            "-----END RSA PRIVATE KEY-----"
        );
        this.publicKeyPem = toPem(
            "-----BEGIN PUBLIC KEY-----",
            this.publicKey.getEncoded(),
            "-----END PUBLIC KEY-----"
        );
        this.publicKeyPemWithRsaHeader = toPem(
            "-----BEGIN RSA PUBLIC KEY-----",
            this.publicKey.getEncoded(),
            "-----END RSA PUBLIC KEY-----"
        );

        this.jwtKeyProperties = new JwtKeyProperties();
    }

    @Test
    @DisplayName("Should create RSA private key from standard PEM headers")
    void shouldCreatePrivateKeyFromStandardPem() {
        this.jwtKeyProperties.setPrivateKeyLocation(
            resource(this.privateKeyPem)
        );

        final RSAPrivateKey parsedPrivateKey = newConfig().jwtPrivateKey();

        assertArrayEquals(
            this.privateKey.getEncoded(),
            parsedPrivateKey.getEncoded()
        );
    }

    @Test
    @DisplayName("Should create RSA private key from RSA PEM headers")
    void shouldCreatePrivateKeyFromRsaPemHeaders() {
        this.jwtKeyProperties.setPrivateKeyLocation(
            resource(this.privateKeyPemWithRsaHeader)
        );

        final RSAPrivateKey parsedPrivateKey = newConfig().jwtPrivateKey();

        assertArrayEquals(
            this.privateKey.getEncoded(),
            parsedPrivateKey.getEncoded()
        );
    }

    @Test
    @DisplayName("Should create RSA public key from standard PEM headers")
    void shouldCreatePublicKeyFromStandardPem() {
        this.jwtKeyProperties.setPublicKeyLocation(resource(this.publicKeyPem));

        final RSAPublicKey parsedPublicKey = newConfig().jwtPublicKey();

        assertArrayEquals(
            this.publicKey.getEncoded(),
            parsedPublicKey.getEncoded()
        );
    }

    @Test
    @DisplayName("Should create RSA public key from RSA PEM headers")
    void shouldCreatePublicKeyFromRsaPemHeaders() {
        this.jwtKeyProperties.setPublicKeyLocation(
            resource(this.publicKeyPemWithRsaHeader)
        );

        final RSAPublicKey parsedPublicKey = newConfig().jwtPublicKey();

        assertArrayEquals(
            this.publicKey.getEncoded(),
            parsedPublicKey.getEncoded()
        );
    }

    @Test
    @DisplayName("Should create RSA public key from plain base64 content")
    void shouldCreatePublicKeyFromBase64Content() {
        this.jwtKeyProperties.setPublicKeyLocation(
            resource(
                Base64.getEncoder().encodeToString(this.publicKey.getEncoded())
            )
        );

        final RSAPublicKey parsedPublicKey = newConfig().jwtPublicKey();

        assertArrayEquals(
            this.publicKey.getEncoded(),
            parsedPublicKey.getEncoded()
        );
    }

    @Test
    @DisplayName(
        "Should throw IllegalStateException when private key cannot be read"
    )
    void shouldThrowWhenPrivateKeyResourceCannotBeRead() {
        this.jwtKeyProperties.setPrivateKeyLocation(new UnreadableResource());
        final JwtKeyConfig jwtKeyConfig = newConfig();

        final IllegalStateException exception = assertThrows(
            IllegalStateException.class,
            jwtKeyConfig::jwtPrivateKey
        );

        assertTrue(
            exception
                .getMessage()
                .contains(
                    "Failed to read app.security.jwt.private-key-location"
                )
        );
        assertNotNull(exception.getCause());
    }

    @Test
    @DisplayName(
        "Should throw IllegalArgumentException when private key content is empty"
    )
    void shouldThrowWhenPrivateKeyContentIsEmpty() {
        this.jwtKeyProperties.setPrivateKeyLocation(resource(" \n\t "));
        final JwtKeyConfig jwtKeyConfig = newConfig();

        final IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            jwtKeyConfig::jwtPrivateKey
        );

        assertTrue(
            exception
                .getMessage()
                .contains(
                    "resolved resource is empty or missing PEM key content"
                )
        );
    }

    @Test
    @DisplayName(
        "Should throw IllegalArgumentException when public key base64 is invalid"
    )
    void shouldThrowWhenPublicKeyBase64IsInvalid() {
        this.jwtKeyProperties.setPublicKeyLocation(
            resource(
                """
                -----BEGIN PUBLIC KEY-----
                not-base64
                -----END PUBLIC KEY-----
                """
            )
        );
        final JwtKeyConfig jwtKeyConfig = newConfig();

        final IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            jwtKeyConfig::jwtPublicKey
        );

        assertTrue(
            exception
                .getMessage()
                .contains("key content is not valid PEM/base64")
        );
    }

    @Test
    @DisplayName(
        "Should throw IllegalArgumentException for unexpected PEM delimiters"
    )
    void shouldThrowWhenPemDelimitersAreUnexpected() {
        this.jwtKeyProperties.setPrivateKeyLocation(
            resource(
                """
                -----BEGIN CERTIFICATE-----
                Zm9v
                -----END CERTIFICATE-----
                """
            )
        );
        final JwtKeyConfig jwtKeyConfig = newConfig();

        final IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            jwtKeyConfig::jwtPrivateKey
        );

        assertTrue(
            exception
                .getMessage()
                .contains(
                    "resolved resource contains unexpected PEM header/footer"
                )
        );
    }

    @Test
    @DisplayName(
        "Should throw IllegalArgumentException when private key does not contain PKCS#8 content"
    )
    void shouldThrowWhenPrivateKeySpecIsInvalid() {
        this.jwtKeyProperties.setPrivateKeyLocation(
            resource(
                toPem(
                    "-----BEGIN PRIVATE KEY-----",
                    this.publicKey.getEncoded(),
                    "-----END PRIVATE KEY-----"
                )
            )
        );
        final JwtKeyConfig jwtKeyConfig = newConfig();

        final IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            jwtKeyConfig::jwtPrivateKey
        );

        assertTrue(
            exception
                .getMessage()
                .contains(
                    "must resolve to a PEM-encoded PKCS#8 RSA private key"
                )
        );
    }

    @Test
    @DisplayName(
        "Should throw IllegalArgumentException when public key does not contain X.509 content"
    )
    void shouldThrowWhenPublicKeySpecIsInvalid() {
        this.jwtKeyProperties.setPublicKeyLocation(
            resource(
                toPem(
                    "-----BEGIN PUBLIC KEY-----",
                    this.privateKey.getEncoded(),
                    "-----END PUBLIC KEY-----"
                )
            )
        );
        final JwtKeyConfig jwtKeyConfig = newConfig();

        final IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            jwtKeyConfig::jwtPublicKey
        );

        assertTrue(
            exception
                .getMessage()
                .contains("must resolve to a PEM-encoded X.509 RSA public key")
        );
    }

    private JwtKeyConfig newConfig() {
        return new JwtKeyConfig(this.jwtKeyProperties);
    }

    private static KeyPair generateRsaKeyPair()
        throws NoSuchAlgorithmException {
        final KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(
            "RSA"
        );
        keyPairGenerator.initialize(2048);
        return keyPairGenerator.generateKeyPair();
    }

    private static String toPem(
        final String header,
        final byte[] encodedKey,
        final String footer
    ) {
        final String base64Body = Base64.getMimeEncoder(
            64,
            "\n".getBytes(StandardCharsets.UTF_8)
        ).encodeToString(encodedKey);

        return header + "\n" + base64Body + "\n" + footer + "\n";
    }

    private static ByteArrayResource resource(final String content) {
        return new ByteArrayResource(content.getBytes(StandardCharsets.UTF_8));
    }

    private static final class UnreadableResource extends AbstractResource {

        @Override
        public String getDescription() {
            return "unreadable-resource";
        }

        @Override
        public InputStream getInputStream() throws IOException {
            throw new IOException("simulated I/O failure");
        }
    }
}
