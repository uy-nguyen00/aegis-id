package com.uynguyen.aegis_id.auth.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CodeExchangeRequest {

    @NotBlank(message = "VALIDATION.CODE_EXCHANGE.CODE.NOT_BLANK")
    private String code;
}
