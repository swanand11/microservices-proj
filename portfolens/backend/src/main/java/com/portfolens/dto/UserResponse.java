package com.portfolens.dto;

import com.portfolens.entity.User;
import java.time.Instant;
import java.util.UUID;

public record UserResponse(UUID id, String fullName, String email, Instant createdAt, Instant updatedAt) {
    public static UserResponse from(User user) { return new UserResponse(user.getId(), user.getFullName(), user.getEmail(), user.getCreatedAt(), user.getUpdatedAt()); }
}
