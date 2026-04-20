package com.uynguyen.aegis_id.security;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.io.Resource;
import org.springframework.validation.annotation.Validated;

@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "app.security.jwt")
public class JwtKeyProperties {

    @NotNull
    private Resource privateKeyLocation;

    @NotNull
    private Resource publicKeyLocation;
}
