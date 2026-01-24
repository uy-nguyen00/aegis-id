package com.uynguyen.aegis_id.auth.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RefreshTokenRequest {

    @NotBlank(message = "VALIDATION.REFRESH_TOKEN.NOT_BLANK")
    private String refreshToken;
}
