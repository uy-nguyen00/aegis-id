package com.uynguyen.aegis_id.user.request;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChangePasswordRequest {

    private String oldPassword;
    private String newPassword;
    private String confirmNewPassword;
}
