package com.darkom.user;

import com.darkom.user.dto.AdminUserResponse;
import com.darkom.user.service.AdminUserService;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/users")
public class AdminUserController {

  private final AdminUserService adminUserService;

  public AdminUserController(AdminUserService adminUserService) {
    this.adminUserService = adminUserService;
  }

  @GetMapping
  public List<AdminUserResponse> list() {
    return adminUserService.list();
  }

  @PatchMapping("/{id}/deactivate")
  public AdminUserResponse deactivate(@PathVariable UUID id) {
    return adminUserService.deactivate(id);
  }
}
