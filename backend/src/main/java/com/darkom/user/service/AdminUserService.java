package com.darkom.user.service;

import com.darkom.auth.entity.User;
import com.darkom.auth.repository.UserRepository;
import com.darkom.user.dto.AdminUserResponse;
import com.darkom.user.exception.UserNotFoundException;
import java.time.Clock;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminUserService {

  private final UserRepository userRepository;
  private final Clock clock;

  public AdminUserService(UserRepository userRepository, Clock clock) {
    this.userRepository = userRepository;
    this.clock = clock;
  }

  @Transactional(readOnly = true)
  public List<AdminUserResponse> list() {
    return userRepository.findAll().stream().map(AdminUserResponse::from).toList();
  }

  @Transactional
  public AdminUserResponse deactivate(UUID userId) {
    User user =
        userRepository.findById(userId).orElseThrow(() -> new UserNotFoundException(userId));
    user.setActive(false);
    user.setUpdatedAt(clock.instant());
    return AdminUserResponse.from(userRepository.save(user));
  }
}
