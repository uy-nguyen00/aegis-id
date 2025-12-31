package com.uynguyen.jwt_spring_security.auth.impl;

import com.uynguyen.jwt_spring_security.auth.AuthenticationService;
import com.uynguyen.jwt_spring_security.auth.request.AuthenticationRequest;
import com.uynguyen.jwt_spring_security.auth.request.RefreshTokenRequest;
import com.uynguyen.jwt_spring_security.auth.request.RegistrationRequest;
import com.uynguyen.jwt_spring_security.auth.response.AuthenticationResponse;
import com.uynguyen.jwt_spring_security.exception.BusinessException;
import com.uynguyen.jwt_spring_security.exception.ErrorCode;
import com.uynguyen.jwt_spring_security.role.Role;
import com.uynguyen.jwt_spring_security.role.RoleRepository;
import com.uynguyen.jwt_spring_security.security.JwtService;
import com.uynguyen.jwt_spring_security.user.User;
import com.uynguyen.jwt_spring_security.user.UserMapper;
import com.uynguyen.jwt_spring_security.user.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthenticationServiceImpl implements AuthenticationService {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserMapper userMapper;

    @Override
    @Transactional
    public void register(RegistrationRequest request) {
        // TODO can add a little proxy here (might be over-engineered)
        checkUserEmail(request.getEmail());
        checkUserPhoneNumber(request.getPhoneNumber());
        checkPasswords(request.getPassword(), request.getConfirmPassword());

        final Role userRole = this.roleRepository.findByName(
            "ROLE_USER"
        ).orElseThrow(() ->
            new EntityNotFoundException("Role 'USER' does not exist")
        );

        List<Role> roles = new ArrayList<>();
        roles.add(userRole);

        final User user = this.userMapper.toUser(request);
        user.setRoles(roles);
        log.debug("Saving user {}", user);
        this.userRepository.save(user);
    }

    @Override
    public AuthenticationResponse login(AuthenticationRequest request) {
        final Authentication authentication =
            this.authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                    request.getEmail(),
                    request.getPassword()
                )
            );

        final User user = (User) authentication.getPrincipal();
        final String accessToken = this.jwtService.generateAccessToken(
            user.getUsername()
        );
        final String refreshToken = this.jwtService.generateRefreshToken(
            user.getUsername()
        );
        final String tokenType = "Bearer";

        return AuthenticationResponse.builder()
            .accessToken(accessToken)
            .refreshToken(refreshToken)
            .tokenType(tokenType)
            .build();
    }

    @Override
    public AuthenticationResponse refreshToken(RefreshTokenRequest request) {
        final String newAccessToken = this.jwtService.refreshAccessToken(
            request.getRefreshToken()
        );
        final String tokenType = "Bearer";
        return AuthenticationResponse.builder()
            .accessToken(newAccessToken)
            .refreshToken(request.getRefreshToken())
            .tokenType(tokenType)
            .build();
    }

    private void checkUserEmail(String email) {
        final boolean isEmailRegistered =
            this.userRepository.existsByEmailIgnoreCase(email);

        if (isEmailRegistered) {
            throw new BusinessException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }
    }

    private void checkUserPhoneNumber(String phoneNumber) {
        final boolean isPhoneNumberRegistered =
            this.userRepository.existsByPhoneNumber(phoneNumber);

        if (isPhoneNumberRegistered) {
            throw new BusinessException(ErrorCode.PHONE_ALREADY_EXISTS);
        }
    }

    private void checkPasswords(String password, String confirmPassword) {
        if (password == null || !password.equals(confirmPassword)) {
            throw new BusinessException(ErrorCode.PASSWORDS_MISMATCH);
        }
    }
}
