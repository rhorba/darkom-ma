package com.darkom.property;

import com.darkom.property.dto.UnitRequest;
import com.darkom.property.dto.UnitResponse;
import com.darkom.property.service.UnitService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/units")
public class UnitController {

  private final UnitService unitService;

  public UnitController(UnitService unitService) {
    this.unitService = unitService;
  }

  @PatchMapping("/{id}")
  public UnitResponse update(
      @PathVariable UUID id,
      @AuthenticationPrincipal UUID userId,
      @Valid @RequestBody UnitRequest request) {
    return unitService.update(id, userId, request);
  }

  @PostMapping("/{id}/archive")
  public UnitResponse archive(@PathVariable UUID id, @AuthenticationPrincipal UUID userId) {
    return unitService.archive(id, userId);
  }
}
