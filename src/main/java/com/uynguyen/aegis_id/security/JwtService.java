package com.uynguyen.aegis_id.security;

import com.uynguyen.aegis_id.exception.BusinessException;
import com.uynguyen.aegis_id.exception.ErrorCode;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

    private static final String TOKEN_TYPE = "token_type";
    private static final String ROLES_CLAIM = "roles";

    private final PrivateKey privateKey;
    private final PublicKey publicKey;

    @Value("${app.security.jwt.access-token-expiration}")
    private long accessTokenExpiration;

    @Value("${app.security.jwt.refresh-token-expiration}")
    private long refreshTokenExpiration;

    @Value("${app.security.jwt.issuer}")
    private String issuer;

    @Value("${app.security.jwt.audience}")
    private String audience;

    public JwtService(
        @Value("${app.security.jwt.private-key}") String privateKeyBase64,
        @Value("${app.security.jwt.public-key}") String publicKeyBase64
    ) {
        final KeyFactory keyFactory;
        try {
            keyFactory = KeyFactory.getInstance("RSA");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(
                "RSA algorithm is not available in this JVM",
                e
            );
        }

        try {
            byte[] privateDecoded = Base64.getDecoder().decode(
                privateKeyBase64
            );
            this.privateKey = keyFactory.generatePrivate(
                new PKCS8EncodedKeySpec(privateDecoded)
            );
        } catch (IllegalArgumentException | InvalidKeySpecException e) {
            throw new IllegalArgumentException(
                "Invalid value for app.security.jwt.private-key (JWT_PRIVATE_KEY): " +
                    "must be a Base64-encoded PKCS#8 RSA private key",
                e
            );
        }

        try {
            byte[] publicDecoded = Base64.getDecoder().decode(publicKeyBase64);
            this.publicKey = keyFactory.generatePublic(
                new X509EncodedKeySpec(publicDecoded)
            );
        } catch (IllegalArgumentException | InvalidKeySpecException e) {
            throw new IllegalArgumentException(
                "Invalid value for app.security.jwt.public-key (JWT_PUBLIC_KEY): " +
                    "must be a Base64-encoded X.509 RSA public key",
                e
            );
        }
    }

    public String generateAccessToken(
        final String userId,
        final List<String> roles
    ) {
        final Map<String, Object> claims = new HashMap<>();
        claims.put(TOKEN_TYPE, "ACCESS_TOKEN");
        claims.put(ROLES_CLAIM, roles);
        return buildToken(userId, claims, this.accessTokenExpiration);
    }

    public String generateRefreshToken(
        final String userId,
        final List<String> roles
    ) {
        final Map<String, Object> claims = new HashMap<>();
        claims.put(TOKEN_TYPE, "REFRESH_TOKEN");
        claims.put(ROLES_CLAIM, roles);
        return buildToken(userId, claims, this.refreshTokenExpiration);
    }

    private String buildToken(
        final String userId,
        final Map<String, Object> claims,
        long tokenExpiration
    ) {
        return Jwts.builder()
            .claims(claims)
            .subject(userId)
            .issuer(this.issuer)
            .audience()
            .add(this.audience)
            .and()
            .issuedAt(new Date(System.currentTimeMillis()))
            .expiration(new Date(System.currentTimeMillis() + tokenExpiration))
            .signWith(this.privateKey)
            .compact();
    }

    public boolean isTokenValid(
        final String token,
        final String expectedUserId
    ) {
        final String userId = extractUserIdFromToken(token);
        return userId.equals(expectedUserId) && !isTokenExpired(token);
    }

    private boolean isTokenExpired(final String token) {
        return extractClaims(token).getExpiration().before(new Date());
    }

    public String extractUserIdFromToken(String token) {
        return extractClaims(token).getSubject();
    }

    @SuppressWarnings("unchecked")
    public List<String> extractRolesFromToken(String token) {
        return extractClaims(token).get(ROLES_CLAIM, List.class);
    }

    private Claims extractClaims(String token) {
        try {
            return Jwts.parser()
                .verifyWith(this.publicKey)
                .requireIssuer(this.issuer)
                .requireAudience(this.audience)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        } catch (final JwtException _) {
            throw new BusinessException(ErrorCode.INVALID_JWT_TOKEN);
        }
    }

    @SuppressWarnings("unchecked")
    public String refreshAccessToken(final String refreshToken) {
        final Claims claims = extractClaims(refreshToken);

        if (!"REFRESH_TOKEN".equals(claims.get(TOKEN_TYPE))) {
            throw new BusinessException(ErrorCode.INVALID_JWT_TOKEN);
        }

        if (isTokenExpired(refreshToken)) {
            throw new BusinessException(ErrorCode.INVALID_JWT_TOKEN);
        }

        final String userId = claims.getSubject();
        final List<String> roles = claims.get(ROLES_CLAIM, List.class);
        return generateAccessToken(userId, roles);
    }
}
