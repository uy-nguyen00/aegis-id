package com.uynguyen.aegis_id.user;

import com.uynguyen.aegis_id.user.request.ChangePasswordRequest;
import com.uynguyen.aegis_id.user.request.ProfileUpdateRequest;
import org.springframework.security.core.userdetails.UserDetailsService;

public interface UserService extends UserDetailsService {
    void updateProfileInfo(ProfileUpdateRequest request, String userId);

    void changePassword(ChangePasswordRequest request, String userId);

    void deactivateAccount(String userId);

    void reactivateAccount(String userId);

    void deleteAccount(String userId);
}
