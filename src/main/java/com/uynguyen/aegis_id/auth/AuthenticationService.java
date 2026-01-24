package com.uynguyen.aegis_id.auth;

import com.uynguyen.aegis_id.auth.request.AuthenticationRequest;
import com.uynguyen.aegis_id.auth.request.RefreshTokenRequest;
import com.uynguyen.aegis_id.auth.request.RegistrationRequest;
import com.uynguyen.aegis_id.auth.response.AuthenticationResponse;

public interface AuthenticationService {
    void register(RegistrationRequest request);

    AuthenticationResponse login(AuthenticationRequest request);

    AuthenticationResponse refreshToken(RefreshTokenRequest request);
}
