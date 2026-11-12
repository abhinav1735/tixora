
package com.tixora.dto.auth;

import com.tixora.entity.Role;

public record AuthResponse(
        String accessToken,
        String tokenType,
        Long userId,
        String fullName,
        String email,
        Role role
) {
}
