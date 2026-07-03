package com.darkom.auth.dto;

import com.darkom.auth.entity.Role;
import com.darkom.auth.entity.User;
import java.util.UUID;

public record UserSummary(UUID id, String email, String fullName, Role role) {

  public static UserSummary from(User user) {
    return new UserSummary(user.getId(), user.getEmail(), user.getFullName(), user.getRole());
  }
}
