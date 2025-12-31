package com.uynguyen.jwt_spring_security.user;

import com.uynguyen.jwt_spring_security.auth.request.RegistrationRequest;
import com.uynguyen.jwt_spring_security.user.request.ProfileUpdateRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserMapper {

    private final PasswordEncoder passwordEncoder;

    public void mergeUserInfo(
        final User user,
        final ProfileUpdateRequest request
    ) {
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setDateOfBirth(request.getDateOfBirth());
    }

    public User toUser(final RegistrationRequest request) {
        return User.builder()
            .firstName(request.getFirstName())
            .lastName(request.getLastName())
            .email(request.getEmail())
            .phoneNumber(request.getPhoneNumber())
            .password(this.passwordEncoder.encode(request.getPassword()))
            .enabled(true) // can change after to implement email / phone verification
            .locked(false)
            .credentialsExpired(false)
            .emailVerified(false)
            .phoneVerified(false)
            .build();
    }
}
