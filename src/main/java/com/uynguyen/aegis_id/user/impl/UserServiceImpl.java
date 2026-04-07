package com.uynguyen.aegis_id.user.impl;

import com.uynguyen.aegis_id.exception.BusinessException;
import com.uynguyen.aegis_id.exception.ErrorCode;
import com.uynguyen.aegis_id.user.User;
import com.uynguyen.aegis_id.user.UserMapper;
import com.uynguyen.aegis_id.user.UserRepository;
import com.uynguyen.aegis_id.user.UserService;
import com.uynguyen.aegis_id.user.request.ChangePasswordRequest;
import com.uynguyen.aegis_id.user.request.ProfileUpdateRequest;
import java.util.Objects;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
        return this.userRepository.findWithRolesByEmailIgnoreCase(
            userEmail
        ).orElseThrow(() ->
            new UsernameNotFoundException(
                "User not found with user email : " + userEmail
            )
        );
    }

    @Override
    public void updateProfileInfo(
        final ProfileUpdateRequest request,
        @NonNull final String userId
    ) {
        final User savedUser = Objects.requireNonNull(
            this.userRepository.findById(userId).orElseThrow(() ->
                new BusinessException(ErrorCode.USER_NOT_FOUND, userId)
            )
        );

        this.userMapper.mergeUserInfo(savedUser, request);
        this.userRepository.save(savedUser);
    }

    @Override
    public void changePassword(
        ChangePasswordRequest request,
        @NonNull String userId
    ) {
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
    public void deactivateAccount(@NonNull String userId) {
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
    public void deleteAccount(String userId) {
        final User user = this.userRepository.findById(userId).orElseThrow(() ->
            new BusinessException(ErrorCode.USER_NOT_FOUND, userId)
        );

        this.userRepository.delete(user);
    }
}
