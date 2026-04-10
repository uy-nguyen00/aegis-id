package com.uynguyen.aegis_id.security;

import java.util.List;

public record TokenUserInfo(
    String userId,
    List<String> roles,
    String firstName,
    String lastName,
    String email
) {}
