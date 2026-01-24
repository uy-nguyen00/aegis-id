package com.uynguyen.aegis_id.auth;

import com.uynguyen.aegis_id.auth.request.AuthenticationRequest;
import com.uynguyen.aegis_id.auth.request.RefreshTokenRequest;
import com.uynguyen.aegis_id.auth.request.RegistrationRequest;
import com.uynguyen.aegis_id.auth.response.AuthenticationResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Authentication API")
public class AuthenticationController {

    private final AuthenticationService authenticationService;

    @PostMapping("/login")
    public ResponseEntity<AuthenticationResponse> login(
        @Valid @RequestBody final AuthenticationRequest request
    ) {
        return ResponseEntity.ok(this.authenticationService.login(request));
    }

    @PostMapping("/register")
    public ResponseEntity<Void> register(
        @Valid @RequestBody final RegistrationRequest request
    ) {
        this.authenticationService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthenticationResponse> refresh(
        @Valid @RequestBody final RefreshTokenRequest request
    ) {
        return ResponseEntity.ok(
            this.authenticationService.refreshToken(request)
        );
    }
}
