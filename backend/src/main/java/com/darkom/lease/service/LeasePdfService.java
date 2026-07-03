package com.darkom.lease.service;

import com.darkom.common.config.StorageProperties;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.stereotype.Service;

/**
 * Generates a lease PDF. Per docs/prd-darkom.md's risk mitigation, the document is explicitly
 * marked as a draft until a legal resource reviews the template - it is not meant to be a
 * signature-ready contract yet.
 */
@Service
public class LeasePdfService {

  public static final String TEMPLATE_VERSION = "v1-draft";

  private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
  private static final float MARGIN = 60f;
  private static final float LINE_HEIGHT = 18f;

  private final StorageProperties storageProperties;

  public LeasePdfService(StorageProperties storageProperties) {
    this.storageProperties = storageProperties;
  }

  public String generate(UUID leaseId, LeaseDocumentData data) throws IOException {
    Path directory = Path.of(storageProperties.getLeaseDocumentsDir());
    Files.createDirectories(directory);
    Path filePath = directory.resolve(leaseId + ".pdf");

    try (PDDocument document = new PDDocument()) {
      PDPage page = new PDPage(PDRectangle.A4);
      document.addPage(page);

      PDType1Font bold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
      PDType1Font regular = new PDType1Font(Standard14Fonts.FontName.HELVETICA);

      try (PDPageContentStream stream = new PDPageContentStream(document, page)) {
        float y = page.getMediaBox().getHeight() - MARGIN;

        y = writeLine(stream, bold, 16, MARGIN, y, "CONTRAT DE BAIL");
        y -= LINE_HEIGHT / 2;
        y =
            writeLine(
                stream,
                bold,
                10,
                MARGIN,
                y,
                "DOCUMENT PROVISOIRE - A FAIRE VALIDER PAR UN JURISTE AVANT SIGNATURE");
        y -= LINE_HEIGHT;

        y = writeLine(stream, bold, 12, MARGIN, y, "Bien loue");
        y =
            writeLine(
                stream, regular, 11, MARGIN, y, data.propertyName() + " - " + data.unitLabel());
        y = writeLine(stream, regular, 11, MARGIN, y, data.propertyAddress());
        y -= LINE_HEIGHT / 2;

        y = writeLine(stream, bold, 12, MARGIN, y, "Bailleur");
        y = writeLine(stream, regular, 11, MARGIN, y, data.landlordFullName());
        y -= LINE_HEIGHT / 2;

        y = writeLine(stream, bold, 12, MARGIN, y, "Locataire");
        y =
            writeLine(
                stream,
                regular,
                11,
                MARGIN,
                y,
                data.tenantFullName() + " (" + data.tenantEmail() + ")");
        y -= LINE_HEIGHT / 2;

        y = writeLine(stream, bold, 12, MARGIN, y, "Duree et loyer");
        y =
            writeLine(
                stream,
                regular,
                11,
                MARGIN,
                y,
                "Du "
                    + data.startDate().format(DATE_FORMAT)
                    + " au "
                    + data.endDate().format(DATE_FORMAT));
        writeLine(stream, regular, 11, MARGIN, y, "Loyer mensuel: " + data.monthlyRent() + " MAD");
      }

      document.save(filePath.toFile());
    }

    return filePath.toString();
  }

  private float writeLine(
      PDPageContentStream stream, PDType1Font font, float fontSize, float x, float y, String text)
      throws IOException {
    stream.beginText();
    stream.setFont(font, fontSize);
    stream.newLineAtOffset(x, y);
    stream.showText(text);
    stream.endText();
    return y - LINE_HEIGHT;
  }
}
