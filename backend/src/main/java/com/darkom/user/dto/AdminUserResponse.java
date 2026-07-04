package com.darkom.user.dto;

import com.darkom.auth.entity.Role;
import com.darkom.auth.entity.User;
import java.util.UUID;

public record AdminUserResponse(UUID id, String email, String fullName, Role role, boolean active) {

  public static AdminUserResponse from(User user) {
    return new AdminUserResponse(
        user.getId(), user.getEmail(), user.getFullName(), user.getRole(), user.isActive());
  }
}
