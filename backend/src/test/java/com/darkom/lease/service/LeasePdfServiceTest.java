package com.darkom.lease.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.darkom.common.config.StorageProperties;
import java.io.File;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.UUID;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class LeasePdfServiceTest {

  @TempDir Path tempDir;

  @Test
  void generatesAReadablePdfContainingLeaseDetails() throws Exception {
    StorageProperties properties = new StorageProperties();
    properties.setLeaseDocumentsDir(tempDir.toString());
    LeasePdfService service = new LeasePdfService(properties);

    UUID leaseId = UUID.randomUUID();
    LeaseDocumentData data =
        new LeaseDocumentData(
            "Villa Zaytouna",
            "12 Rue des Oliviers, Rabat",
            "Apt 1",
            "Rachid Landlord",
            "Sara Tenant",
            "sara@example.com",
            LocalDate.of(2026, 1, 1),
            LocalDate.of(2026, 12, 31),
            new BigDecimal("3500.00"));

    String path = service.generate(leaseId, data);

    File file = new File(path);
    assertThat(file).exists();
    assertThat(file.getName()).isEqualTo(leaseId + ".pdf");

    try (PDDocument document = Loader.loadPDF(file)) {
      String text = new PDFTextStripper().getText(document);
      assertThat(text)
          .contains("CONTRAT DE BAIL")
          .contains("DOCUMENT PROVISOIRE")
          .contains("Villa Zaytouna")
          .contains("Apt 1")
          .contains("Rachid Landlord")
          .contains("Sara Tenant")
          .contains("sara@example.com")
          .contains("3500.00");
    }
  }
}
