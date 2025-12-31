package com.uynguyen.jwt_spring_security.user.impl;

import com.uynguyen.jwt_spring_security.exception.BusinessException;
import com.uynguyen.jwt_spring_security.exception.ErrorCode;
import com.uynguyen.jwt_spring_security.user.User;
import com.uynguyen.jwt_spring_security.user.UserMapper;
import com.uynguyen.jwt_spring_security.user.UserRepository;
import com.uynguyen.jwt_spring_security.user.UserService;
import com.uynguyen.jwt_spring_security.user.request.ChangePasswordRequest;
import com.uynguyen.jwt_spring_security.user.request.ProfileUpdateRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;

    @Override
    public UserDetails loadUserByUsername(final String userEmail)
        throws UsernameNotFoundException {
        return this.userRepository.findByEmailIgnoreCase(userEmail).orElseThrow(
            () ->
                new UsernameNotFoundException(
                    "User not found with user email : " + userEmail
                )
        );

        //        return this.userRepository.findByEmailIgnoreCase(userEmail)
        //                .orElseThrow(new Supplier<UsernameNotFoundException>() {
        //                    @Override
        //                    public UsernameNotFoundException get() {
        //                        return new UsernameNotFoundException("User not found with user email : " + userEmail);
        //                    }
        //                });
    }

    @Override
    public void updateProfileInfo(
        final ProfileUpdateRequest request,
        final String userId
    ) {
        final User savedUser = this.userRepository.findById(userId).orElseThrow(
            () -> new BusinessException(ErrorCode.USER_NOT_FOUND, userId)
        );

        this.userMapper.mergeUserInfo(savedUser, request);
        this.userRepository.save(savedUser);
    }

    @Override
    public void changePassword(ChangePasswordRequest request, String userId) {
        if (!request.getNewPassword().equals(request.getConfirmNewPassword())) {
            throw new BusinessException(ErrorCode.CHANGE_PASSWORD_MISMATCH);
        }

        final User savedUser = this.userRepository.findById(userId).orElseThrow(
            () -> new BusinessException(ErrorCode.USER_NOT_FOUND, userId)
        );

        if (
            !this.passwordEncoder.matches(
                request.getOldPassword(),
                savedUser.getPassword()
            )
        ) {
            throw new BusinessException(ErrorCode.INVALID_OLD_PASSWORD);
        }

        final String encodedPassword = this.passwordEncoder.encode(
            request.getNewPassword()
        );
        savedUser.setPassword(encodedPassword);
        this.userRepository.save(savedUser);
    }

    @Override
    public void deactivateAccount(String userId) {
        final User user = this.userRepository.findById(userId).orElseThrow(() ->
            new BusinessException(ErrorCode.USER_NOT_FOUND, userId)
        );

        if (!user.isEnabled()) {
            throw new BusinessException(ErrorCode.ACCOUNT_ALREADY_DEACTIVATED);
        }

        user.setEnabled(false);
        this.userRepository.save(user);
    }

    @Override
    public void reactivateAccount(@NonNull String userId) {
        final User user = this.userRepository.findById(userId).orElseThrow(() ->
            new BusinessException(ErrorCode.USER_NOT_FOUND, userId)
        );

        if (user.isEnabled()) {
            throw new BusinessException(ErrorCode.ACCOUNT_ALREADY_ACTIVATED);
        }

        user.setEnabled(true);
        this.userRepository.save(user);
    }

    @Override
    public void deleteAccount(String userId) {}
}
