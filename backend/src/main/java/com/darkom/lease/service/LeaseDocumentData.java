package com.darkom.lease.service;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Everything the PDF needs to print, resolved by LeaseService before generation. */
public record LeaseDocumentData(
    String propertyName,
    String propertyAddress,
    String unitLabel,
    String landlordFullName,
    String tenantFullName,
    String tenantEmail,
    LocalDate startDate,
    LocalDate endDate,
    BigDecimal monthlyRent) {}
