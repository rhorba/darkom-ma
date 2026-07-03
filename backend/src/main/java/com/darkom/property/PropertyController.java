package com.darkom.property;

import com.darkom.property.dto.PropertyRequest;
import com.darkom.property.dto.PropertyResponse;
import com.darkom.property.dto.UnitRequest;
import com.darkom.property.dto.UnitResponse;
import com.darkom.property.service.PropertyService;
import com.darkom.property.service.UnitService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/properties")
public class PropertyController {

  private final PropertyService propertyService;
  private final UnitService unitService;

  public PropertyController(PropertyService propertyService, UnitService unitService) {
    this.propertyService = propertyService;
    this.unitService = unitService;
  }

  @GetMapping
  public List<PropertyResponse> list(@AuthenticationPrincipal UUID userId) {
    return propertyService.listAccessibleTo(userId);
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public PropertyResponse create(
      @AuthenticationPrincipal UUID userId, @Valid @RequestBody PropertyRequest request) {
    return propertyService.create(userId, request);
  }

  @GetMapping("/{id}")
  public PropertyResponse get(@PathVariable UUID id, @AuthenticationPrincipal UUID userId) {
    return propertyService.get(id, userId);
  }

  @PatchMapping("/{id}")
  public PropertyResponse update(
      @PathVariable UUID id,
      @AuthenticationPrincipal UUID userId,
      @Valid @RequestBody PropertyRequest request) {
    return propertyService.update(id, userId, request);
  }

  @PostMapping("/{id}/archive")
  public PropertyResponse archive(@PathVariable UUID id, @AuthenticationPrincipal UUID userId) {
    return propertyService.archive(id, userId);
  }

  @GetMapping("/{id}/units")
  public List<UnitResponse> listUnits(@PathVariable UUID id, @AuthenticationPrincipal UUID userId) {
    return unitService.listForProperty(id, userId);
  }

  @PostMapping("/{id}/units")
  @ResponseStatus(HttpStatus.CREATED)
  public UnitResponse createUnit(
      @PathVariable UUID id,
      @AuthenticationPrincipal UUID userId,
      @Valid @RequestBody UnitRequest request) {
    return unitService.create(id, userId, request);
  }
}
