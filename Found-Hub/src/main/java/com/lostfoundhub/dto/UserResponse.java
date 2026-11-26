package com.lostfoundhub.dto;

import com.lostfoundhub.enums.UserRole;
import com.lostfoundhub.model.User;

import java.time.LocalDateTime;

/** Safe view of a user - the password hash is never included. */
public record UserResponse(Long id, String name, String email, String phone, UserRole role, LocalDateTime createdAt) {

    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getName(), user.getEmail(), user.getPhone(),
                user.getRole(), user.getCreatedAt());
    }
}
