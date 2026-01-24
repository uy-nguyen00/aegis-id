package com.uynguyen.aegis_id.security;

import com.uynguyen.aegis_id.exception.BusinessException;
import com.uynguyen.aegis_id.exception.ErrorCode;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import java.security.PrivateKey;
import java.security.PublicKey;
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

    public JwtService() throws Exception {
        this.privateKey = KeyUtils.loadPrivateKey(
            "keys/local-only/private_key.pem"
        );
        this.publicKey = KeyUtils.loadPublicKey(
            "keys/local-only/public_key.pem"
        );
    }

    public String generateAccessToken(
        final String username,
        final List<String> roles
    ) {
        final Map<String, Object> claims = new HashMap<>();
        claims.put(TOKEN_TYPE, "ACCESS_TOKEN");
        claims.put(ROLES_CLAIM, roles);
        return buildToken(username, claims, this.accessTokenExpiration);
    }

    public String generateRefreshToken(
        final String username,
        final List<String> roles
    ) {
        final Map<String, Object> claims = new HashMap<>();
        claims.put(TOKEN_TYPE, "REFRESH_TOKEN");
        claims.put(ROLES_CLAIM, roles);
        return buildToken(username, claims, this.refreshTokenExpiration);
    }

    private String buildToken(
        final String username,
        final Map<String, Object> claims,
        long tokenExpiration
    ) {
        return Jwts.builder()
            .claims(claims)
            .subject(username)
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
        final String expectedUsername
    ) {
        final String username = extractUsernameFromToken(token);
        return username.equals(expectedUsername) && !isTokenExpired(token);
    }

    private boolean isTokenExpired(final String token) {
        return extractClaims(token).getExpiration().before(new Date());
    }

    public String extractUsernameFromToken(String token) {
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
        } catch (final JwtException e) {
            throw new BusinessException(ErrorCode.INVALID_JWT_TOKEN);
        }
    }

    @SuppressWarnings("unchecked")
    public String refreshAccessToken(final String refreshToken) {
        final Claims claims = extractClaims(refreshToken);

        if (!claims.get(TOKEN_TYPE).equals("REFRESH_TOKEN")) {
            throw new RuntimeException("Invalid token type");
        }

        if (isTokenExpired(refreshToken)) {
            throw new RuntimeException("Refresh token expired");
        }

        final String username = claims.getSubject();
        final List<String> roles = claims.get(ROLES_CLAIM, List.class);
        return generateAccessToken(username, roles);
    }
}
