package com.uynguyen.aegis_id.security;

import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.boot.actuate.endpoint.annotation.WriteOperation;
import org.springframework.stereotype.Component;

@Component
@Endpoint(id = "jwtconfig")
@RequiredArgsConstructor
public class JwtConfigEndpoint {

    private final JwtService jwtService;

    @ReadOperation
    public Map<String, Object> read() {
        return Map.of("includeRolesClaim", jwtService.isIncludeRolesClaim());
    }

    @WriteOperation
    public Map<String, Object> write(boolean includeRolesClaim) {
        jwtService.setIncludeRolesClaim(includeRolesClaim);
        return Map.of("includeRolesClaim", jwtService.isIncludeRolesClaim());
    }
}
