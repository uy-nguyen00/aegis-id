package com.uynguyen.aegis_id.auth;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuthorizationCodeRepository
    extends JpaRepository<AuthorizationCode, String> {
    Optional<AuthorizationCode> findByCode(String code);
}
