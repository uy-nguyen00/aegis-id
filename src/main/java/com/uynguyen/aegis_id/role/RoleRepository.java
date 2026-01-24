package com.uynguyen.aegis_id.role;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoleRepository extends JpaRepository<Role, String> {
    Optional<Role> findByName(String name);
}
