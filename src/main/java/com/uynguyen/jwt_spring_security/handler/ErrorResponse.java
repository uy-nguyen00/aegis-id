package com.uynguyen.jwt_spring_security.handler;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;
import lombok.*;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString
public class ErrorResponse {

    private String message;
    private String code;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<ValidationError> validationErrorList;
}
