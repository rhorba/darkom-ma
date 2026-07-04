package com.darkom.maintenance;

import com.darkom.maintenance.dto.MaintenanceRequestResponse;
import com.darkom.maintenance.dto.MaintenanceStatusUpdateRequest;
import com.darkom.maintenance.service.MaintenanceService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/maintenance")
@Validated
public class MaintenanceController {

  private final MaintenanceService maintenanceService;

  public MaintenanceController(MaintenanceService maintenanceService) {
    this.maintenanceService = maintenanceService;
  }

  @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @ResponseStatus(HttpStatus.CREATED)
  public MaintenanceRequestResponse create(
      @AuthenticationPrincipal UUID userId,
      @RequestParam UUID unitId,
      @RequestParam @NotBlank String description,
      @RequestParam(required = false) MultipartFile photo) {
    return maintenanceService.create(unitId, userId, description, photo);
  }

  @GetMapping("/mine")
  public List<MaintenanceRequestResponse> listMine(@AuthenticationPrincipal UUID userId) {
    return maintenanceService.listMine(userId);
  }

  @GetMapping
  public List<MaintenanceRequestResponse> list(@AuthenticationPrincipal UUID userId) {
    return maintenanceService.listForLandlord(userId);
  }

  @PatchMapping("/{id}")
  public MaintenanceRequestResponse updateStatus(
      @PathVariable UUID id,
      @AuthenticationPrincipal UUID userId,
      @Valid @RequestBody MaintenanceStatusUpdateRequest request) {
    return maintenanceService.updateStatus(id, userId, request.status());
  }

  @GetMapping("/{id}/photo")
  public ResponseEntity<Resource> downloadPhoto(
      @PathVariable UUID id, @AuthenticationPrincipal UUID userId) {
    Path path = maintenanceService.getPhotoPath(id, userId);
    Resource resource = new FileSystemResource(path);
    return ResponseEntity.ok().contentType(mediaTypeFor(path)).body(resource);
  }

  private MediaType mediaTypeFor(Path path) {
    String name = path.getFileName().toString().toLowerCase();
    if (name.endsWith(".png")) {
      return MediaType.IMAGE_PNG;
    }
    if (name.endsWith(".webp")) {
      return MediaType.parseMediaType("image/webp");
    }
    return MediaType.IMAGE_JPEG;
  }
}
