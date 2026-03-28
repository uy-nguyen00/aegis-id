package com.uynguyen.aegis_id.auth;

import com.uynguyen.aegis_id.auth.request.AuthenticationRequest;
import com.uynguyen.aegis_id.auth.request.AuthorizeRequest;
import com.uynguyen.aegis_id.auth.request.CodeExchangeRequest;
import com.uynguyen.aegis_id.auth.request.RefreshTokenRequest;
import com.uynguyen.aegis_id.auth.request.RegistrationRequest;
import com.uynguyen.aegis_id.auth.response.AuthenticationResponse;
import com.uynguyen.aegis_id.auth.response.AuthorizeResponse;

public interface AuthenticationService {
    void register(RegistrationRequest request);

    AuthenticationResponse login(AuthenticationRequest request);

    AuthenticationResponse refreshToken(RefreshTokenRequest request);

    AuthorizeResponse authorize(AuthorizeRequest request);

    AuthenticationResponse exchangeCode(CodeExchangeRequest request);
}
