package com.darkom.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.darkom.auth.entity.Role;
import com.darkom.auth.entity.User;
import com.darkom.auth.repository.UserRepository;
import com.darkom.user.exception.UserNotFoundException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AdminUserServiceTest {

  @Mock private UserRepository userRepository;

  private final Clock clock = Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC);
  private AdminUserService adminUserService;

  @BeforeEach
  void setUp() {
    adminUserService = new AdminUserService(userRepository, clock);
  }

  private User user(UUID id, boolean active) {
    User user = new User();
    user.setId(id);
    user.setEmail("user@example.com");
    user.setFullName("Test User");
    user.setRole(Role.TENANT);
    user.setActive(active);
    return user;
  }

  @Test
  void listReturnsAllUsers() {
    when(userRepository.findAll()).thenReturn(List.of(user(UUID.randomUUID(), true)));

    var result = adminUserService.list();

    assertThat(result).hasSize(1);
    assertThat(result.get(0).active()).isTrue();
  }

  @Test
  void deactivateMarksTheUserInactive() {
    UUID userId = UUID.randomUUID();
    User user = user(userId, true);
    when(userRepository.findById(userId)).thenReturn(Optional.of(user));
    when(userRepository.save(user)).thenReturn(user);

    var result = adminUserService.deactivate(userId);

    assertThat(result.active()).isFalse();
    assertThat(user.isActive()).isFalse();
  }

  @Test
  void deactivateThrowsWhenUserDoesNotExist() {
    UUID userId = UUID.randomUUID();
    when(userRepository.findById(userId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> adminUserService.deactivate(userId))
        .isInstanceOf(UserNotFoundException.class);
  }
}
