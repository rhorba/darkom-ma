package com.darkom.lease;

import com.darkom.lease.dto.LeaseRequest;
import com.darkom.lease.dto.LeaseResponse;
import com.darkom.lease.service.LeaseService;
import jakarta.validation.Valid;
import java.nio.file.Path;
import java.util.UUID;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/leases")
public class LeaseController {

  private final LeaseService leaseService;

  public LeaseController(LeaseService leaseService) {
    this.leaseService = leaseService;
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public LeaseResponse create(
      @AuthenticationPrincipal UUID userId, @Valid @RequestBody LeaseRequest request) {
    return leaseService.create(userId, request);
  }

  @GetMapping("/{id}")
  public LeaseResponse get(@PathVariable UUID id, @AuthenticationPrincipal UUID userId) {
    return leaseService.get(id, userId);
  }

  @GetMapping("/{id}/document")
  public ResponseEntity<Resource> downloadDocument(
      @PathVariable UUID id, @AuthenticationPrincipal UUID userId) {
    Path path = leaseService.getDocumentPath(id, userId);
    Resource resource = new FileSystemResource(path);

    return ResponseEntity.ok()
        .contentType(MediaType.APPLICATION_PDF)
        .header(
            HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + path.getFileName() + "\"")
        .body(resource);
  }
}
