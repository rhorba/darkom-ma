package com.darkom.maintenance.service;

import com.darkom.common.config.StorageProperties;
import com.darkom.maintenance.exception.InvalidPhotoException;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/** Disk-based storage, mirroring LeasePdfService's pattern - fine for a localhost MVP (SDR-2). */
@Service
public class MaintenancePhotoStorage {

  private static final Set<String> ALLOWED_CONTENT_TYPES =
      Set.of("image/jpeg", "image/png", "image/webp");

  private final StorageProperties storageProperties;

  public MaintenancePhotoStorage(StorageProperties storageProperties) {
    this.storageProperties = storageProperties;
  }

  public String store(UUID requestId, MultipartFile photo) {
    String contentType = photo.getContentType();
    if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType)) {
      throw new InvalidPhotoException(
          "Unsupported photo type - only JPEG, PNG, or WebP images are accepted");
    }

    try {
      Path directory = Path.of(storageProperties.getMaintenancePhotosDir());
      Files.createDirectories(directory);
      Path filePath = directory.resolve(requestId + extensionFor(contentType));
      photo.transferTo(filePath);
      return filePath.toString();
    } catch (IOException e) {
      throw new UncheckedIOException("Failed to store maintenance photo", e);
    }
  }

  private String extensionFor(String contentType) {
    return switch (contentType) {
      case "image/jpeg" -> ".jpg";
      case "image/png" -> ".png";
      case "image/webp" -> ".webp";
      default -> "";
    };
  }
}
