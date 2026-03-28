package com.uynguyen.aegis_id.auth;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.*;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "AUTHORIZATION_CODES")
public class AuthorizationCode {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "CODE", nullable = false, unique = true, length = 64)
    private String code;

    @Column(name = "USER_ID", nullable = false)
    private String userId;

    @Column(name = "REDIRECT_URI", nullable = false, length = 2048)
    private String redirectUri;

    @Column(name = "EXPIRES_AT", nullable = false)
    private Instant expiresAt;

    @Column(name = "USED", nullable = false)
    private boolean used;
}
