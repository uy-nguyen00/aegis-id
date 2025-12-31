package com.uynguyen.jwt_spring_security.auth;

import com.uynguyen.jwt_spring_security.auth.request.AuthenticationRequest;
import com.uynguyen.jwt_spring_security.auth.request.RefreshTokenRequest;
import com.uynguyen.jwt_spring_security.auth.request.RegistrationRequest;
import com.uynguyen.jwt_spring_security.auth.response.AuthenticationResponse;

public interface AuthenticationService {
    void register(RegistrationRequest request);

    AuthenticationResponse login(AuthenticationRequest request);

    AuthenticationResponse refreshToken(RefreshTokenRequest request);
}
